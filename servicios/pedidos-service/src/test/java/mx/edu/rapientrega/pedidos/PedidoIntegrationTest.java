package mx.edu.rapientrega.pedidos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.*;
import java.util.*;
import mx.edu.rapientrega.pedidos.dtos.*;
import mx.edu.rapientrega.pedidos.repositories.*;
import mx.edu.rapientrega.pedidos.services.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:pedidos;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.messaging=false",
      "app.jobs=false"
    })
@Import(HttpTestConfig.class)
@Transactional
class PedidoIntegrationTest {
  @Autowired DominioService service;
  @Autowired PedidoRepository pedidos;
  @Autowired OutboxRepository outbox;
  @Autowired MockRestServiceServer http;
  @MockitoBean Clock clock;
  Instant now = Instant.parse("2026-10-23T12:00:00Z");

  @BeforeEach
  void setup() {
    when(clock.instant()).thenReturn(now);
    http.reset();
  }

  RegistrarPedido solicitud() {
    return new RegistrarPedido(
        "cliente-1",
        1L,
        List.of(new RegistrarPedido.Item(1L, 2)),
        new RegistrarPedido.Entrega(19.43, -99.13, "Campus"));
  }

  String crear() {
    http.expect(requestTo("http://localhost:8081/api/v1/restaurantes/1/menu"))
        .andRespond(
            withSuccess(
                "[{\"id\":1,\"restauranteId\":1,\"nombre\":\"Tacos\",\"precio\":65.50}]",
                MediaType.APPLICATION_JSON));
    http.expect(requestTo("http://localhost:8081/api/v1/restaurantes"))
        .andRespond(
            withSuccess(
                "[{\"id\":1,\"nombre\":\"Cocina\",\"latitud\":19.43,\"longitud\":-99.13}]",
                MediaType.APPLICATION_JSON));
    return service.registrarPedido(UUID.randomUUID().toString(), solicitud()).id;
  }

  void pagar(String id) {
    service.consumir(
        new Evento(
            UUID.randomUUID().toString(),
            "pago.confirmado.v1",
            1,
            now,
            id,
            Map.of("pagoId", "pago-1")));
  }

  @Test
  void pedidoImporteYPlazo() {
    var id = crear();
    assertEquals(new java.math.BigDecimal("131.00"), service.consultar(id).total);
    pagar(id);
    assertEquals(now.plusSeconds(300), service.consultar(id).limite);
    http.verify();
  }

  @Test
  void aceptaAntesDelLimite() {
    var id = crear();
    pagar(id);
    when(clock.instant()).thenReturn(now.plusSeconds(299));
    assertEquals("ACEPTADO", service.decidir(id, true).estado);
    assertEquals(1, outbox.count());
  }

  @Test
  void rechazaYEmiteUnaCancelacion() {
    var id = crear();
    pagar(id);
    assertEquals("CANCELADO", service.decidir(id, false).estado);
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.decidir(id, false));
    assertEquals(1, outbox.count());
  }

  @Test
  void igualdadDelLimiteNoAcepta() {
    var id = crear();
    pagar(id);
    when(clock.instant()).thenReturn(now.plusSeconds(300));
    assertEquals("CANCELADO", service.decidir(id, true).estado);
    service.expirarPedido();
    assertEquals(1, outbox.count());
  }

  @Test
  void expiraTrasCincoMinutos() {
    var id = crear();
    pagar(id);
    when(clock.instant()).thenReturn(now.plusSeconds(301));
    service.expirarPedido();
    assertEquals("CANCELADO", service.consultar(id).estado);
  }

  @Test
  void pagoRechazadoNoIniciaEspera() {
    var id = crear();
    service.consumir(new Evento("rechazo", "pago.rechazado.v1", 1, now, id, Map.of()));
    assertEquals("PAGO_RECHAZADO", service.consultar(id).estado);
    assertNull(service.consultar(id).limite);
  }

  @Test
  void eventoDuplicadoNoReiniciaPlazo() {
    var id = crear();
    var e = new Evento("e1", "pago.confirmado.v1", 1, now, id, Map.of("pagoId", "p1"));
    service.consumir(e);
    when(clock.instant()).thenReturn(now.plusSeconds(60));
    service.consumir(e);
    assertEquals(now.plusSeconds(300), service.consultar(id).limite);
  }

  @Test
  void registrarEsIdempotente() {
    var id = crear();
    var p = service.consultar(id);
    assertEquals(id, service.registrarPedido(p.clave, solicitud()).id);
  }
}
