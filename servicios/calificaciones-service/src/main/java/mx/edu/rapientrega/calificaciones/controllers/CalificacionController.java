package mx.edu.rapientrega.calificaciones.controllers;

import jakarta.validation.Valid;
import mx.edu.rapientrega.calificaciones.dtos.RegistrarCalificacion;
import mx.edu.rapientrega.calificaciones.models.Calificacion;
import mx.edu.rapientrega.calificaciones.services.DominioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/calificaciones")
public class CalificacionController {
  private final DominioService s;

  public CalificacionController(DominioService s) {
    this.s = s;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Calificacion registrarCalificacion(@Valid @RequestBody RegistrarCalificacion c) {
    return s.registrarCalificacion(c);
  }

  @GetMapping("/{id}")
  public Calificacion consultar(@PathVariable String id) {
    return s.consultar(id);
  }
}
