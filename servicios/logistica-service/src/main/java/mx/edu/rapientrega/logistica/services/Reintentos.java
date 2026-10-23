package mx.edu.rapientrega.logistica.services;

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
public class Reintentos {
  private final DominioService s;

  public Reintentos(DominioService s) {
    this.s = s;
  }

  @Scheduled(fixedDelay = 10000)
  public void ejecutar() {
    s.reintentar();
  }
}
