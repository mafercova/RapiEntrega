package mx.edu.rapientrega.calificaciones;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.*;
import java.util.*;
import mx.edu.rapientrega.calificaciones.dtos.*;
import mx.edu.rapientrega.calificaciones.repositories.*;
import mx.edu.rapientrega.calificaciones.services.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:calificaciones;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.eventos-habilitados=false",
      "app.jobs=false"
    })
@Transactional
class CalificacionIntegrationTest {
  @Autowired DominioService service;

  @Test
  void calificaSoloEntregado() {
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.registrarCalificacion(new RegistrarCalificacion("p1", "c1", 5, "Bien")));
    service.consumir(
        new Evento("e1", "pedido.entregado.v1", 1, Instant.now(), "p1", Map.of("clienteId", "c1")));
    var s = new RegistrarCalificacion("p1", "c1", 5, "Bien");
    assertEquals(5, service.registrarCalificacion(s).puntuacion);
    assertEquals(5, service.registrarCalificacion(s).puntuacion);
  }

  @Test
  void clienteAjenoRechazado() {
    service.consumir(
        new Evento("e1", "pedido.entregado.v1", 1, Instant.now(), "p1", Map.of("clienteId", "c1")));
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.registrarCalificacion(new RegistrarCalificacion("p1", "otro", 5, "Bien")));
  }
}
