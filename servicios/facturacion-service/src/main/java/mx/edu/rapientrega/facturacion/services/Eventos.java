package mx.edu.rapientrega.facturacion.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.*;
import mx.edu.rapientrega.facturacion.dtos.Evento;
import mx.edu.rapientrega.facturacion.models.Outbox;
import mx.edu.rapientrega.facturacion.repositories.OutboxRepository;
import org.springframework.stereotype.Service;

@Service
public class Eventos {
  private final OutboxRepository repo;
  private final ObjectMapper json;
  private final Clock clock;

  public Eventos(OutboxRepository r, ObjectMapper j, Clock c) {
    repo = r;
    json = j;
    clock = c;
  }

  public void publicar(String key, String id, Map<String, Object> payload) {
    try {
      repo.save(
          new Outbox(
              key,
              json.writeValueAsString(
                  new Evento(UUID.randomUUID().toString(), key, 1, clock.instant(), id, payload))));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
