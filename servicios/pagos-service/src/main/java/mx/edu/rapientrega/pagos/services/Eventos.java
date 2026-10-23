package mx.edu.rapientrega.pagos.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.*;
import mx.edu.rapientrega.pagos.dtos.Evento;
import mx.edu.rapientrega.pagos.models.Outbox;
import mx.edu.rapientrega.pagos.repositories.OutboxRepository;
import org.springframework.stereotype.Service;

@Service
public class Eventos {
  private static final Map<String, List<String>> SUSCRIPTORES =
      Map.ofEntries(
          Map.entry("pago.confirmado.v1", List.of("pedidos")),
          Map.entry("pago.rechazado.v1", List.of("pedidos")),
          Map.entry("pedido.aceptado.v1", List.of("logistica")),
          Map.entry("pedido.cancelado.v1", List.of("pagos")),
          Map.entry("asignacion.pendiente.v1", List.of("pedidos")),
          Map.entry("repartidor.asignado.v1", List.of("pedidos")),
          Map.entry("pedido.entregado.v1", List.of("pedidos", "facturacion", "calificaciones")));
  private final OutboxRepository repo;
  private final ObjectMapper json;
  private final Clock clock;

  public Eventos(OutboxRepository r, ObjectMapper j, Clock c) {
    repo = r;
    json = j;
    clock = c;
  }

  public void publicar(String tipo, String id, Map<String, Object> datos) {
    try {
      var evento = new Evento(UUID.randomUUID().toString(), tipo, 1, clock.instant(), id, datos);
      var cuerpo = json.writeValueAsString(evento);
      var destinos = SUSCRIPTORES.get(tipo);
      if (destinos == null) throw new IllegalArgumentException("EVENTO_NO_SOPORTADO");
      for (String destino : destinos) repo.save(new Outbox(tipo, cuerpo, destino));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
