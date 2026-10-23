package mx.edu.rapientrega.logistica.services;

import mx.edu.rapientrega.logistica.repositories.OutboxRepository;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.messaging", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
  private final OutboxRepository repo;
  private final RabbitTemplate rabbit;

  public OutboxPublisher(OutboxRepository r, RabbitTemplate t) {
    repo = r;
    rabbit = t;
    rabbit.setMandatory(true);
  }

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void enviar() {
    for (var o : repo.pendientes()) {
      try {
        var c = new CorrelationData(o.id);
        var p = new MessageProperties();
        p.setContentType("application/json");
        p.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        rabbit.send(
            "rapientrega.eventos",
            o.routingKey,
            new Message(o.payload.getBytes(java.nio.charset.StandardCharsets.UTF_8), p),
            c);
        if (!c.getFuture().get(5, java.util.concurrent.TimeUnit.SECONDS).isAck()
            || c.getReturned() != null) break;
        o.enviado = true;
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(getClass()).warn("Publicación pendiente {}", o.id);
        break;
      }
    }
  }
}
