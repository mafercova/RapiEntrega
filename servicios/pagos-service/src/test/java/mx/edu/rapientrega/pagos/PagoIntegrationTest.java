package mx.edu.rapientrega.pagos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.*;
import java.util.*;
import mx.edu.rapientrega.pagos.dtos.*;
import mx.edu.rapientrega.pagos.repositories.*;
import mx.edu.rapientrega.pagos.services.*;
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
      "spring.datasource.url=jdbc:h2:mem:pagos;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.eventos-habilitados=false",
      "app.jobs=false"
    })
@Import(HttpTestConfig.class)
@Transactional
class PagoIntegrationTest {
  @Autowired DominioService service;
  @Autowired PagoRepository pagos;
  @Autowired MockRestServiceServer http;
  @MockitoBean Clock clock;
  Instant now = Instant.parse("2026-10-23T12:00:00Z");

  @BeforeEach
  void setup() {
    when(clock.instant()).thenReturn(now);
    http.reset();
  }

  void pedido() {
    http.expect(requestTo("http://localhost:8082/api/v1/pedidos/pedido1"))
        .andRespond(
            withSuccess(
                "{\"id\":\"pedido1\",\"estado\":\"PAGO_PENDIENTE\",\"total\":131.00}",
                MediaType.APPLICATION_JSON));
  }

  void cobro(String estado) {
    pedido();
    http.expect(requestTo("http://localhost:8091/mock/cobros"))
        .andExpect(header("Idempotency-Key", "charge-pedido1"))
        .andRespond(
            withSuccess(
                "{\"referencia\":\"ext-1\",\"estado\":\"" + estado + "\",\"simulacion\":true}",
                MediaType.APPLICATION_JSON));
  }

  @Test
  void pagoAutorizadoIdempotente() {
    cobro("AUTORIZADO");
    var p = service.procesarPago("clave", new ProcesarPago("pedido1", "ok"));
    assertEquals("AUTORIZADO", p.estado);
    assertEquals(p.id, service.procesarPago("clave", new ProcesarPago("pedido1", "ok")).id);
    http.verify();
  }

  @Test
  void pagoRechazado() {
    cobro("RECHAZADO");
    assertEquals(
        "RECHAZADO", service.procesarPago("clave", new ProcesarPago("pedido1", "reject")).estado);
  }

  @Test
  void respuestaPerdidaConsultaAntesDeRecobrar() {
    pedido();
    http.expect(requestTo("http://localhost:8091/mock/cobros")).andRespond(withServerError());
    http.expect(requestTo("http://localhost:8091/mock/operaciones/charge-pedido1"))
        .andRespond(
            withSuccess(
                "{\"referencia\":\"ext-1\",\"estado\":\"AUTORIZADO\",\"simulacion\":true}",
                MediaType.APPLICATION_JSON));
    assertEquals(
        "AUTORIZADO",
        service.procesarPago("clave", new ProcesarPago("pedido1", "timeout-once")).estado);
    http.verify();
  }

  @Test
  void reembolsoExactamenteUnaVezAnteEventoDuplicado() {
    cobro("AUTORIZADO");
    var p = service.procesarPago("clave", new ProcesarPago("pedido1", "ok"));
    var e = new Evento("cancel1", "pedido.cancelado.v1", 1, now, "pedido1", Map.of());
    service.consumir(e);
    service.consumir(e);
    http.verify();
    http.reset();
    http.expect(requestTo("http://localhost:8091/mock/reembolsos/" + p.id))
        .andExpect(header("Idempotency-Key", "refund-" + p.id))
        .andRespond(
            withSuccess(
                "{\"referencia\":\"refund1\",\"estado\":\"REEMBOLSADO\",\"simulacion\":true}",
                MediaType.APPLICATION_JSON));
    service.reintentarReembolsos();
    service.reintentarReembolsos();
    assertEquals("REEMBOLSADO", service.consultar("pedido1").estado);
    http.verify();
  }

  @Test
  void falloReembolsoReintentaConMismaClave() {
    cobro("AUTORIZADO");
    var p = service.procesarPago("clave", new ProcesarPago("pedido1", "ok"));
    service.consumir(new Evento("cancel", "pedido.cancelado.v1", 1, now, "pedido1", Map.of()));
    http.verify();
    http.reset();
    http.expect(requestTo("http://localhost:8091/mock/reembolsos/" + p.id))
        .andRespond(withServerError());
    http.expect(requestTo("http://localhost:8091/mock/reembolsos/" + p.id))
        .andExpect(header("Idempotency-Key", "refund-" + p.id))
        .andRespond(
            withSuccess(
                "{\"referencia\":\"refund1\",\"estado\":\"REEMBOLSADO\",\"simulacion\":true}",
                MediaType.APPLICATION_JSON));
    service.reintentarReembolsos();
    assertEquals("REEMBOLSO_PENDIENTE", service.consultar("pedido1").estado);
    when(clock.instant()).thenReturn(now.plusSeconds(5));
    service.reintentarReembolsos();
    assertEquals("REEMBOLSADO", service.consultar("pedido1").estado);
    http.verify();
  }
}
