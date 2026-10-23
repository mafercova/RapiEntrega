package mx.edu.rapientrega.pagos.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "app.jobs",
    havingValue = "true",
    matchIfMissing = true)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "app.jobs",
    havingValue = "true",
    matchIfMissing = true)
@Component
public class Reembolsos {
  private final DominioService s;

  public Reembolsos(DominioService s) {
    this.s = s;
  }

  @Scheduled(fixedDelay = 1000)
  public void ejecutar() {
    s.reintentarReembolsos();
  }
}
