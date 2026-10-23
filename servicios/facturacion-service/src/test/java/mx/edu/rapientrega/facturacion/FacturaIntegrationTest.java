package mx.edu.rapientrega.facturacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.*;
import java.util.*;
import mx.edu.rapientrega.facturacion.dtos.*;
import mx.edu.rapientrega.facturacion.repositories.*;
import mx.edu.rapientrega.facturacion.services.*;
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
      "spring.datasource.url=jdbc:h2:mem:facturacion;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.eventos-habilitados=false",
      "app.jobs=false"
    })
@Import(HttpTestConfig.class)
@Transactional
class FacturaIntegrationTest {
  @Autowired DominioService service;
  @Autowired MockRestServiceServer http;
  @MockitoBean Clock clock;
  Instant now = Instant.parse("2026-10-23T12:00:00Z");

  @BeforeEach
  void setup() {
    when(clock.instant()).thenReturn(now);
    http.reset();
  }

  @Test
  void falloDeLegadoReintentaConClaveEstable() {
    http.expect(requestTo("http://localhost:8092/legacy2012/comprobantes"))
        .andRespond(withServerError());
    http.expect(requestTo("http://localhost:8092/legacy2012/comprobantes"))
        .andExpect(header("Idempotency-Key", "invoice-p1"))
        .andRespond(
            withSuccess("{\"NUM_FOLIO\":\"F1\",\"SIMULACION\":true}", MediaType.APPLICATION_JSON));
    var e = new Evento("e1", "pedido.entregado.v1", 1, now, "p1", Map.of("clienteId", "c1"));
    service.consumir(e);
    service.consumir(e);
    service.emitirPendientes();
    assertEquals("PENDIENTE", service.consultar("p1").estado);
    when(clock.instant()).thenReturn(now.plusSeconds(5));
    service.emitirPendientes();
    assertEquals("F1", service.consultar("p1").folio);
    http.verify();
  }
}
