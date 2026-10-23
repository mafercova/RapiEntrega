package mx.edu.rapientrega.logistica.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.edu.rapientrega.logistica.dtos.Evento;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.messaging", havingValue = "true", matchIfMissing = true)
public class EventConsumer {
  private final DominioService service;
  private final ObjectMapper json;

  public EventConsumer(DominioService s, ObjectMapper j) {
    service = s;
    json = j;
  }

  @RabbitListener(queues = "logistica.eventos")
  public void recibir(String raw) throws Exception {
    var evento = json.readValue(raw, Evento.class);
    if (evento.version() != 1) throw new IllegalArgumentException("EVENT_VERSION_UNSUPPORTED");
    service.consumir(evento);
  }
}
