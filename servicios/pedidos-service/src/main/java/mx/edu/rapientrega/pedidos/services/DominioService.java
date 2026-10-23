package mx.edu.rapientrega.pedidos.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import mx.edu.rapientrega.pedidos.dtos.*;
import mx.edu.rapientrega.pedidos.models.*;
import mx.edu.rapientrega.pedidos.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DominioService {
  private final PedidoRepository repo;
  private final InboxRepository inbox;
  private final Eventos eventos;
  private final RestClient http;
  private final ObjectMapper json;
  private final Clock clock;
  private final String catalogo;

  public record Restaurante(Long id, String nombre, double latitud, double longitud) {}

  public record Producto(Long id, Long restauranteId, String nombre, BigDecimal precio) {}

  public DominioService(
      PedidoRepository r,
      InboxRepository i,
      Eventos e,
      RestClient h,
      ObjectMapper j,
      Clock c,
      @Value("${app.catalogo-url}") String u) {
    repo = r;
    inbox = i;
    eventos = e;
    http = h;
    json = j;
    clock = c;
    catalogo = u;
  }

  public static ResponseStatusException error(HttpStatus s, String c) {
    return new ResponseStatusException(s, c);
  }

  public Pedido consultar(String id) {
    return repo.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "PEDIDO_NO_EXISTE"));
  }

  private String encode(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public Pedido registrarPedido(String clave, RegistrarPedido s) {
    if (clave.isBlank() || clave.length() > 128)
      throw error(HttpStatus.BAD_REQUEST, "CLAVE_INVALIDA");
    String body = encode(s);
    var previo = repo.findByClave(clave);
    if (previo.isPresent()) {
      if (!previo.get().solicitud.equals(body))
        throw error(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT");
      return previo.get();
    }
    var menu =
        http.get()
            .uri(catalogo + "/api/v1/restaurantes/{id}/menu", s.restauranteId())
            .retrieve()
            .body(Producto[].class);
    var restaurantes =
        http.get().uri(catalogo + "/api/v1/restaurantes").retrieve().body(Restaurante[].class);
    var restaurante =
        Arrays.stream(Objects.requireNonNull(restaurantes))
            .filter(x -> x.id().equals(s.restauranteId()))
            .findFirst()
            .orElseThrow(() -> error(HttpStatus.UNPROCESSABLE_ENTITY, "RESTAURANTE_NO_DISPONIBLE"));
    var precios = new HashMap<Long, Producto>();
    for (var p : Objects.requireNonNull(menu)) precios.put(p.id(), p);
    var seen = new HashSet<Long>();
    var partidas = new ArrayList<Map<String, Object>>();
    BigDecimal total = BigDecimal.ZERO;
    for (var item : s.items()) {
      if (!seen.add(item.productoId()))
        throw error(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCTO_DUPLICADO");
      var p = precios.get(item.productoId());
      if (p == null) throw error(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCTO_NO_DISPONIBLE");
      total = total.add(p.precio().multiply(BigDecimal.valueOf(item.cantidad())));
      partidas.add(
          Map.of("productoId", p.id(), "cantidad", item.cantidad(), "precioUnitario", p.precio()));
    }
    if (total.compareTo(new BigDecimal("9999999999.99")) > 0)
      throw error(HttpStatus.UNPROCESSABLE_ENTITY, "IMPORTE_EXCESIVO");
    var p = new Pedido(UUID.randomUUID().toString());
    p.clave = clave;
    p.solicitud = body;
    p.clienteId = s.clienteId();
    p.restauranteId = s.restauranteId();
    p.total = total.setScale(2);
    p.estado = "PAGO_PENDIENTE";
    p.latitud = s.entrega().latitud();
    p.longitud = s.entrega().longitud();
    p.recogidaLat = restaurante.latitud();
    p.recogidaLon = restaurante.longitud();
    p.direccion = s.entrega().direccion();
    p.partidas = encode(partidas);
    return repo.save(p);
  }

  @Transactional
  public Pedido decidir(String id, boolean aceptar) {
    var p = repo.bloquear(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "PEDIDO_NO_EXISTE"));
    if (!"PENDIENTE_ACEPTACION".equals(p.estado))
      throw error(HttpStatus.CONFLICT, "ESTADO_INVALIDO");
    if (!clock.instant().isBefore(p.limite)) {
      cancelar(p, "PLAZO_VENCIDO");
      return p;
    }
    if (!aceptar) {
      cancelar(p, "RECHAZO_RESTAURANTE");
      return p;
    }
    p.estado = "ACEPTADO";
    eventos.publicar(
        "pedido.aceptado.v1",
        p.id,
        Map.of(
            "pedidoId",
            p.id,
            "clienteId",
            p.clienteId,
            "restauranteId",
            p.restauranteId,
            "latitud",
            p.recogidaLat,
            "longitud",
            p.recogidaLon));
    return p;
  }

  private void cancelar(Pedido p, String motivo) {
    p.estado = "CANCELADO";
    eventos.publicar(
        "pedido.cancelado.v1",
        p.id,
        Map.of("pedidoId", p.id, "pagoId", p.pagoId, "motivo", motivo));
  }

  @Transactional
  public void expirarPedido() {
    for (var candidato :
        repo.findByEstadoAndLimiteLessThanEqual("PENDIENTE_ACEPTACION", clock.instant())) {
      var p = repo.bloquear(candidato.id).orElseThrow();
      if ("PENDIENTE_ACEPTACION".equals(p.estado) && !clock.instant().isBefore(p.limite))
        cancelar(p, "PLAZO_VENCIDO");
    }
  }

  @Transactional
  public void consumir(Evento e) {
    if (inbox.existsById(e.eventId())) return;
    var p =
        repo.bloquear(e.aggregateId())
            .orElseThrow(() -> error(HttpStatus.CONFLICT, "EVENTO_SIN_PEDIDO"));
    switch (e.eventType()) {
      case "pago.confirmado.v1" -> {
        if ("PAGO_PENDIENTE".equals(p.estado)) {
          p.pagoId = e.payload().get("pagoId").toString();
          p.estado = "PENDIENTE_ACEPTACION";
          p.pendienteAceptacionAt = clock.instant();
          p.limite = p.pendienteAceptacionAt.plusSeconds(300);
        }
      }
      case "pago.rechazado.v1" -> {
        if ("PAGO_PENDIENTE".equals(p.estado)) p.estado = "PAGO_RECHAZADO";
      }
      case "asignacion.pendiente.v1" -> {
        if ("ACEPTADO".equals(p.estado)) p.estado = "ASIGNACION_PENDIENTE";
      }
      case "repartidor.asignado.v1" -> {
        if (Set.of("ACEPTADO", "ASIGNACION_PENDIENTE").contains(p.estado)) {
          p.estado = "ASIGNADO";
          p.repartidorId = e.payload().get("repartidorId").toString();
        }
      }
      case "pedido.entregado.v1" -> {
        if (!"ENTREGADO".equals(p.estado)) {
          if (!Set.of("ACEPTADO", "ASIGNACION_PENDIENTE", "ASIGNADO").contains(p.estado))
            throw error(HttpStatus.CONFLICT, "ENTREGA_FUERA_DE_ORDEN");
          p.estado = "ENTREGADO";
        }
      }
      default -> throw error(HttpStatus.BAD_REQUEST, "EVENTO_NO_SOPORTADO");
    }
    inbox.save(new Inbox(e.eventId()));
  }
}
