package mx.edu.rapientrega.pedidos;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.*;
import java.util.*;
import mx.edu.rapientrega.pedidos.repositories.*;
import mx.edu.rapientrega.pedidos.services.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:entregas-http;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.eventos-habilitados=false",
      "app.jobs=false"
    })
@Transactional
class EntregaEventosIntegrationTest {
  @Autowired Eventos eventos;
  @Autowired OutboxRepository outbox;

  @Test
  void publicacionTieneEntregaIndependientePorSuscriptor() {
    eventos.publicar("pedido.entregado.v1", "p1", Map.of("clienteId", "c1"));
    assertEquals(3, outbox.count());
    assertEquals(
        Set.of("pedidos", "facturacion", "calificaciones"),
        outbox.findAll().stream()
            .map(o -> o.destinatario)
            .collect(java.util.stream.Collectors.toSet()));
  }

  @Test
  void falloDeUnSuscriptorNoDuplicaEntregasYaConfirmadas() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
    var env = new MockEnvironment();
    for (var d : List.of("pedidos", "facturacion", "calificaciones"))
      env.setProperty("app." + d + "-url", "http://" + d);
    eventos.publicar("pedido.entregado.v1", "p1", Map.of("clienteId", "c1"));
    server.expect(requestTo("http://pedidos/interno/eventos")).andRespond(withNoContent());
    server.expect(requestTo("http://calificaciones/interno/eventos")).andRespond(withNoContent());
    server.expect(requestTo("http://facturacion/interno/eventos")).andRespond(withServerError());
    var now = Instant.parse("2026-10-23T12:00:00Z");
    var worker =
        new OutboxPublisher(outbox, builder.build(), env, Clock.fixed(now, ZoneOffset.UTC));
    worker.enviar();
    server.verify();
    assertEquals(2, outbox.findAll().stream().filter(o -> o.enviado).count());
    server.reset();
    server.expect(requestTo("http://facturacion/interno/eventos")).andRespond(withNoContent());
    new OutboxPublisher(
            outbox, builder.build(), env, Clock.fixed(now.plusSeconds(3), ZoneOffset.UTC))
        .enviar();
    server.verify();
    assertTrue(outbox.findAll().stream().allMatch(o -> o.enviado));
  }
}
