package mx.edu.rapientrega.pedidos.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.*;
import mx.edu.rapientrega.pedidos.dtos.Evento;
import mx.edu.rapientrega.pedidos.models.Outbox;
import mx.edu.rapientrega.pedidos.repositories.OutboxRepository;
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
