package mx.edu.rapientrega.facturacion.services;

import java.time.Clock;
import java.util.Set;
import mx.edu.rapientrega.facturacion.repositories.OutboxRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(
    name = "app.eventos-habilitados",
    havingValue = "true",
    matchIfMissing = true)
public class OutboxPublisher {
  private final OutboxRepository repo;
  private final RestClient http;
  private final Environment env;
  private final Clock clock;

  public OutboxPublisher(OutboxRepository r, RestClient h, Environment e, Clock c) {
    repo = r;
    http = h;
    env = e;
    clock = c;
  }

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void enviar() {
    for (var entrega : repo.pendientes()) {
      if (entrega.proximoIntento != null && entrega.proximoIntento.isAfter(clock.instant()))
        continue;
      try {
        if (!Set.of("pedidos", "pagos", "logistica", "facturacion", "calificaciones")
            .contains(entrega.destinatario))
          throw new IllegalStateException("DESTINATARIO_INVALIDO");
        String url = env.getRequiredProperty("app." + entrega.destinatario + "-url");
        http.post()
            .uri(url + "/interno/eventos")
            .contentType(MediaType.APPLICATION_JSON)
            .body(entrega.payload)
            .retrieve()
            .toBodilessEntity();
        entrega.enviado = true;
      } catch (Exception e) {
        entrega.intentos = Math.min(entrega.intentos + 1, 30);
        entrega.proximoIntento =
            clock.instant().plusSeconds(Math.min(60, 1L << Math.min(entrega.intentos, 6)));
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("Entrega {} pendiente para {}", entrega.id, entrega.destinatario);
      }
    }
  }
}
