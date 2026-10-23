package mx.edu.rapientrega.calificaciones.controllers;

import java.util.Set;
import mx.edu.rapientrega.calificaciones.dtos.Evento;
import mx.edu.rapientrega.calificaciones.services.DominioService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Endpoint interno: ejecutar en red local de confianza; sin autenticación productiva. */
@RestController
@RequestMapping("/interno/eventos")
public class EventoController {
  private static final Set<String> TIPOS = Set.of("pedido.entregado.v1");
  private final DominioService servicio;

  public EventoController(DominioService s) {
    servicio = s;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void recibir(@RequestBody Evento e) {
    if (e.version() != 1
        || e.eventId() == null
        || e.eventId().isBlank()
        || e.aggregateId() == null
        || e.payload() == null
        || e.eventType() == null
        || !TIPOS.contains(e.eventType()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EVENTO_INVALIDO");
    servicio.consumir(e);
  }
}
