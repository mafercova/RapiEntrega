package mx.edu.rapientrega.logistica.controllers;

import jakarta.validation.Valid;
import mx.edu.rapientrega.logistica.dtos.Ubicacion;
import mx.edu.rapientrega.logistica.models.Asignacion;
import mx.edu.rapientrega.logistica.services.DominioService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asignaciones")
public class LogisticaController {
  private final DominioService s;

  public LogisticaController(DominioService s) {
    this.s = s;
  }

  public record Solicitud(String pedidoId) {}

  public record Entrega(String repartidorId) {}

  @PostMapping
  public ResponseEntity<Asignacion> asignarRepartidor(@RequestBody Solicitud a) {
    var r = s.asignarRepartidor(a.pedidoId());
    return ResponseEntity.status("ASIGNACION_PENDIENTE".equals(r.estado) ? 202 : 200).body(r);
  }

  @GetMapping("/{id}")
  public Asignacion consultar(@PathVariable String id) {
    return s.consultar(id);
  }

  @PostMapping("/{id}/ubicaciones")
  public Asignacion actualizarUbicacion(@PathVariable String id, @Valid @RequestBody Ubicacion u) {
    return s.actualizarUbicacion(id, u);
  }

  @PostMapping("/{id}/entrega")
  public Asignacion completarEntrega(@PathVariable String id, @RequestBody Entrega e) {
    return s.completarEntrega(id, e.repartidorId());
  }
}
