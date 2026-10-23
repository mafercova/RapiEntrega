package mx.edu.rapientrega.pagos.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import mx.edu.rapientrega.pagos.dtos.*;
import mx.edu.rapientrega.pagos.models.*;
import mx.edu.rapientrega.pagos.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DominioService {
  private final PagoRepository repo;
  private final InboxRepository inbox;
  private final Eventos eventos;
  private final PasarelaAdapter pasarela;
  private final RestClient http;
  private final String pedidos;
  private final Clock clock;

  public record PedidoRemoto(String id, String estado, BigDecimal total) {}

  public DominioService(
      PagoRepository r,
      InboxRepository i,
      Eventos e,
      PasarelaAdapter a,
      RestClient h,
      @Value("${app.pedidos-url}") String p,
      Clock c) {
    repo = r;
    inbox = i;
    eventos = e;
    pasarela = a;
    http = h;
    pedidos = p;
    clock = c;
  }

  private ResponseStatusException error(HttpStatus s, String c) {
    return new ResponseStatusException(s, c);
  }

  public Pago consultar(String id) {
    return repo.findByPedidoId(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "PAGO_NO_EXISTE"));
  }

  @Transactional
  public Pago procesarPago(String clave, ProcesarPago s) {
    if (clave.isBlank() || clave.length() > 128)
      throw error(HttpStatus.BAD_REQUEST, "CLAVE_INVALIDA");
    String fingerprint;
    try {
      fingerprint =
          java.util.HexFormat.of()
              .formatHex(
                  java.security.MessageDigest.getInstance("SHA-256")
                      .digest(
                          (s.pedidoId() + "|" + s.tokenTarjeta())
                              .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
    var existente = repo.findByClave(clave);
    if (existente.isPresent()) {
      if (!existente.get().fingerprint.equals(fingerprint))
        throw error(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT");
      return existente.get();
    }
    if (repo.findByPedidoId(s.pedidoId()).isPresent())
      throw error(HttpStatus.CONFLICT, "PEDIDO_YA_PROCESADO");
    var pedido =
        http.get()
            .uri(pedidos + "/api/v1/pedidos/{id}", s.pedidoId())
            .retrieve()
            .body(PedidoRemoto.class);
    if (pedido == null || !"PAGO_PENDIENTE".equals(pedido.estado()))
      throw error(HttpStatus.CONFLICT, "PEDIDO_NO_PAGABLE");
    // Stable provider key derives from order, including concurrent requests with different client
    // keys.
    var result =
        pasarela.cobrar("charge-" + s.pedidoId(), s.pedidoId(), pedido.total(), s.tokenTarjeta());
    var p = new Pago(UUID.randomUUID().toString());
    p.pedidoId = s.pedidoId();
    p.clave = clave;
    p.fingerprint = fingerprint;
    p.importe = pedido.total();
    p.estado = result.estado();
    p.referencia = result.referencia();
    repo.save(p);
    eventos.publicar(
        "AUTORIZADO".equals(p.estado) ? "pago.confirmado.v1" : "pago.rechazado.v1",
        p.pedidoId,
        Map.of("pedidoId", p.pedidoId, "pagoId", p.id, "importe", p.importe, "moneda", "MXN"));
    return p;
  }

  @Transactional
  public void consumir(Evento e) {
    if (inbox.existsById(e.eventId())) return;
    var original =
        repo.findByPedidoId(e.aggregateId())
            .orElseThrow(() -> error(HttpStatus.CONFLICT, "PAGO_AUN_NO_DISPONIBLE"));
    var p = repo.bloquear(original.id).orElseThrow();
    if ("AUTORIZADO".equals(p.estado)) {
      p.estado = "REEMBOLSO_PENDIENTE";
      p.proximoReintento = clock.instant();
    }
    inbox.save(new Inbox(e.eventId()));
  }

  @Transactional
  public void reintentarReembolsos() {
    for (var row : repo.findByEstado("REEMBOLSO_PENDIENTE")) {
      var p = repo.bloquear(row.id).orElseThrow();
      if (!"REEMBOLSO_PENDIENTE".equals(p.estado) || p.proximoReintento.isAfter(clock.instant()))
        continue;
      try {
        var r = pasarela.reembolsar(p.id);
        p.reembolsoReferencia = r.referencia();
        p.estado = "REEMBOLSADO";
      } catch (org.springframework.web.client.RestClientException e) {
        p.reembolsoIntentos++;
        if (p.reembolsoIntentos >= 3) p.estado = "REEMBOLSO_REVISION";
        else p.proximoReintento = clock.instant().plusSeconds(1L << p.reembolsoIntentos);
      }
    }
  }

  @Transactional
  public Pago recuperarReembolso(String pedidoId) {
    var row = consultar(pedidoId);
    var p = repo.bloquear(row.id).orElseThrow();
    if (!"REEMBOLSO_REVISION".equals(p.estado))
      throw error(HttpStatus.CONFLICT, "NO_REQUIERE_RECUPERACION");
    p.estado = "REEMBOLSO_PENDIENTE";
    p.reembolsoIntentos = 0;
    p.proximoReintento = clock.instant();
    return p;
  }
}
