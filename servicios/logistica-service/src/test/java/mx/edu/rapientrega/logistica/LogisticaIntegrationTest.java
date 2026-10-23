package mx.edu.rapientrega.logistica;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.*;
import java.util.*;
import mx.edu.rapientrega.logistica.dtos.*;
import mx.edu.rapientrega.logistica.repositories.*;
import mx.edu.rapientrega.logistica.services.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:logistica;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.messaging=false",
      "spring.rabbitmq.password=unused-test",
      "app.jobs=false"
    })
@Transactional
class LogisticaIntegrationTest {
  @Autowired DominioService service;
  @Autowired RepartidorRepository repartidores;
  @Autowired AsignacionRepository asignaciones;
  @MockitoBean org.springframework.messaging.simp.SimpMessagingTemplate ws;
  @MockitoBean Clock clock;
  Instant now = Instant.parse("2026-10-23T12:00:00Z");

  @BeforeEach
  void setup() {
    when(clock.instant()).thenReturn(now);
  }

  Evento aceptado(String id) {
    return new Evento(
        "e-" + id,
        "pedido.aceptado.v1",
        1,
        now,
        id,
        Map.of("clienteId", "c1", "latitud", 19.4326, "longitud", -99.1332));
  }

  @Test
  void ausenciaDeRepartidores() {
    service.consumir(aceptado("p1"));
    assertEquals("ASIGNACION_PENDIENTE", service.consultar("p1").estado);
  }

  @Test
  void asignaMasCercano() {
    repartidores.save(new mx.edu.rapientrega.logistica.models.Repartidor("lejos", 20, -99));
    repartidores.save(
        new mx.edu.rapientrega.logistica.models.Repartidor("cerca", 19.4327, -99.1333));
    service.consumir(aceptado("p1"));
    assertEquals("cerca", service.consultar("p1").repartidorId);
    service.consumir(aceptado("p2"));
    assertEquals("lejos", service.consultar("p2").repartidorId);
  }

  @Test
  void distanciaConocida() {
    assertEquals(111.195, Distancia.haversine(0, 0, 0, 1), 0.01);
    assertEquals(0, Distancia.haversine(19, -99, 19, -99), 0.00001);
  }

  @Test
  void posicionPersistidaYFueraDeOrdenRechazada() {
    repartidores.save(new mx.edu.rapientrega.logistica.models.Repartidor("r1", 19.43, -99.13));
    service.consumir(aceptado("p1"));
    var u = new Ubicacion("r1", 19.44, -99.14, now, 1);
    assertEquals(1, service.actualizarUbicacion("p1", u).secuencia);
    service.actualizarUbicacion("p1", u);
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.actualizarUbicacion("p1", new Ubicacion("r1", 19.4, -99.1, now, 0)));
  }

  @Test
  void entregaLiberaRepartidor() {
    repartidores.save(new mx.edu.rapientrega.logistica.models.Repartidor("r1", 19.43, -99.13));
    service.consumir(aceptado("p1"));
    service.completarEntrega("p1", "r1");
    assertTrue(repartidores.findById("r1").orElseThrow().disponible);
    assertEquals("ENTREGADO", service.consultar("p1").estado);
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void websocketSeEmiteTrasCommit() {
    repartidores.save(new mx.edu.rapientrega.logistica.models.Repartidor("rws", 19.43, -99.13));
    try {
      service.consumir(aceptado("pws"));
      service.actualizarUbicacion("pws", new Ubicacion("rws", 19.44, -99.14, now, 1));
      verify(ws).convertAndSend(eq("/topic/pedidos/pws"), any(Object.class));
    } finally {
      asignaciones.deleteById("pws");
      repartidores.deleteById("rws");
    }
  }
}
