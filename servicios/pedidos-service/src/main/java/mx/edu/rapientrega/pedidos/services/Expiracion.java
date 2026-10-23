package mx.edu.rapientrega.pedidos.services;

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
public class Expiracion {
  private final DominioService s;

  public Expiracion(DominioService s) {
    this.s = s;
  }

  @Scheduled(fixedDelay = 1000)
  public void revisar() {
    s.expirarPedido();
  }
}
