package mx.edu.rapientrega.logistica.services;

import java.time.Clock;
import java.util.*;
import mx.edu.rapientrega.logistica.dtos.*;
import mx.edu.rapientrega.logistica.models.*;
import mx.edu.rapientrega.logistica.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DominioService {

  private final RepartidorRepository repartidores;

  private final AsignacionRepository asignaciones;

  private final InboxRepository inbox;

  private final Eventos eventos;

  private final SimpMessagingTemplate ws;

  private final Clock clock;

  public DominioService(
      RepartidorRepository r,
      AsignacionRepository a,
      InboxRepository i,
      Eventos e,
      SimpMessagingTemplate w,
      Clock c) {

    repartidores = r;

    asignaciones = a;

    inbox = i;

    eventos = e;

    ws = w;

    clock = c;
  }

  private ResponseStatusException error(HttpStatus s, String c) {

    return new ResponseStatusException(s, c);
  }

  public Asignacion consultar(String id) {

    return asignaciones
        .findById(id)
        .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "ASIGNACION_NO_EXISTE"));
  }

  @Transactional
  public void consumir(Evento e) {

    if (inbox.existsById(e.eventId())) return;

    if (!asignaciones.existsById(e.aggregateId())) {

      var a = new Asignacion(e.aggregateId());

      a.clienteId = e.payload().get("clienteId").toString();

      a.recogidaLat = ((Number) e.payload().get("latitud")).doubleValue();

      a.recogidaLon = ((Number) e.payload().get("longitud")).doubleValue();

      a.estado = "ASIGNACION_PENDIENTE";

      a = asignaciones.saveAndFlush(a);

      asignar(a);
    }

    inbox.save(new Inbox(e.eventId()));
  }

  private void asignar(Asignacion a) {

    var r =
        repartidores.disponibles().stream()
            .min(
                Comparator.comparingDouble(
                        (Repartidor x) ->
                            Distancia.haversine(
                                a.recogidaLat, a.recogidaLon, x.latitud, x.longitud))
                    .thenComparing(x -> x.id));

    if (r.isEmpty()) {

      eventos.publicar(
          "asignacion.pendiente.v1",
          a.pedidoId,
          Map.of("pedidoId", a.pedidoId, "motivo", "SIN_REPARTIDORES"));

      return;
    }

    var rep = r.get();

    rep.disponible = false;

    a.repartidorId = rep.id;

    a.estado = "ASIGNADO";

    a.latitud = rep.latitud;

    a.longitud = rep.longitud;

    a.distanciaKm = Distancia.haversine(a.recogidaLat, a.recogidaLon, rep.latitud, rep.longitud);

    eventos.publicar(
        "repartidor.asignado.v1",
        a.pedidoId,
        Map.of("pedidoId", a.pedidoId, "repartidorId", rep.id));
  }

  @Transactional
  public Asignacion asignarRepartidor(String id) {

    var a =
        asignaciones
            .bloquear(id)
            .orElseThrow(() -> error(HttpStatus.CONFLICT, "PEDIDO_NO_ACEPTADO"));

    if ("ASIGNACION_PENDIENTE".equals(a.estado)) asignar(a);

    return a;
  }

  @Transactional
  public void reintentar() {

    for (var a : asignaciones.findByEstado("ASIGNACION_PENDIENTE")) asignarRepartidor(a.pedidoId);
  }

  @Transactional
  public Asignacion actualizarUbicacion(String id, Ubicacion u) {

    var a =
        asignaciones
            .bloquear(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "ASIGNACION_NO_EXISTE"));

    if (!Objects.equals(a.repartidorId, u.repartidorId()))
      throw error(HttpStatus.FORBIDDEN, "REPARTIDOR_NO_ASIGNADO");

    if (!"ASIGNADO".equals(a.estado)) throw error(HttpStatus.CONFLICT, "ENTREGA_FINALIZADA");

    if (u.capturedAt().isAfter(clock.instant().plusSeconds(30)))
      throw error(HttpStatus.BAD_REQUEST, "FECHA_FUTURA");

    if (u.sequence() == a.secuencia
        && u.latitud() == a.latitud
        && u.longitud() == a.longitud
        && u.capturedAt().equals(a.capturedAt)) return a;

    if (u.sequence() <= a.secuencia) throw error(HttpStatus.CONFLICT, "POSICION_ATRASADA");

    a.latitud = u.latitud();

    a.longitud = u.longitud();

    a.capturedAt = u.capturedAt();

    a.receivedAt = clock.instant();

    a.secuencia = u.sequence();

    var rep = repartidores.bloquear(a.repartidorId).orElseThrow();

    rep.latitud = a.latitud;

    rep.longitud = a.longitud;

    var mensaje =
        Map.of(
            "pedidoId",
            a.pedidoId,
            "latitud",
            a.latitud,
            "longitud",
            a.longitud,
            "sequence",
            a.secuencia,
            "capturedAt",
            a.capturedAt,
            "receivedAt",
            a.receivedAt);

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {

          public void afterCommit() {

            try {
              ws.convertAndSend("/topic/pedidos/" + id, mensaje);
            } catch (org.springframework.messaging.MessagingException e) {
              org.slf4j.LoggerFactory.getLogger(getClass())
                  .warn("Difusión no disponible para {}", id);
            }
          }
        });

    return a;
  }

  @Transactional
  public Asignacion completarEntrega(String id, String rep) {

    var a =
        asignaciones
            .bloquear(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "ASIGNACION_NO_EXISTE"));

    if (!Objects.equals(a.repartidorId, rep))
      throw error(HttpStatus.FORBIDDEN, "REPARTIDOR_NO_ASIGNADO");

    if ("ENTREGADO".equals(a.estado)) return a;

    if (!"ASIGNADO".equals(a.estado)) throw error(HttpStatus.CONFLICT, "ESTADO_INVALIDO");

    a.estado = "ENTREGADO";

    repartidores.bloquear(rep).orElseThrow().disponible = true;

    eventos.publicar(
        "pedido.entregado.v1",
        id,
        Map.of(
            "pedidoId", id, "clienteId", a.clienteId, "deliveredAt", clock.instant().toString()));

    return a;
  }
}
