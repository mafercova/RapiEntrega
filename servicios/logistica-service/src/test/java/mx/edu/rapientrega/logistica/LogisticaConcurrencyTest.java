package mx.edu.rapientrega.logistica;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import mx.edu.rapientrega.logistica.dtos.Evento;
import mx.edu.rapientrega.logistica.models.Repartidor;
import mx.edu.rapientrega.logistica.repositories.*;
import mx.edu.rapientrega.logistica.services.DominioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:concurrencia;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.messaging=false",
      "spring.rabbitmq.password=unused-test",
      "app.jobs=false"
    })
@DirtiesContext
class LogisticaConcurrencyTest {
  @Autowired DominioService service;
  @Autowired RepartidorRepository repartidores;

  @Test
  void reservasConcurrentesNoDuplicanRepartidor() throws Exception {
    repartidores.saveAllAndFlush(
        List.of(new Repartidor("r1", 19.43, -99.13), new Repartidor("r2", 19.5, -99.2)));
    var start = new CountDownLatch(1);
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = new ArrayList<Future<?>>();
      for (String id : List.of("a", "b"))
        futures.add(
            pool.submit(
                () -> {
                  try {
                    start.await();
                    service.consumir(
                        new Evento(
                            "e-" + id,
                            "pedido.aceptado.v1",
                            1,
                            Instant.now(),
                            id,
                            Map.of("clienteId", "c1", "latitud", 19.43, "longitud", -99.13)));
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                }));
      start.countDown();
      for (var f : futures) f.get(20, TimeUnit.SECONDS);
    }
    var a = service.consultar("a");
    var b = service.consultar("b");
    assertEquals("ASIGNADO", a.estado);
    assertEquals("ASIGNADO", b.estado);
    assertNotEquals(a.repartidorId, b.repartidorId);
  }
}
