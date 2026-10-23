package mx.edu.rapientrega.pedidos.controllers;

import jakarta.validation.Valid;
import mx.edu.rapientrega.pedidos.dtos.RegistrarPedido;
import mx.edu.rapientrega.pedidos.models.Pedido;
import mx.edu.rapientrega.pedidos.services.DominioService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {
  private final DominioService service;

  public PedidoController(DominioService s) {
    service = s;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Pedido registrarPedido(
      @RequestHeader("Idempotency-Key") String k, @Valid @RequestBody RegistrarPedido s) {
    return service.registrarPedido(k, s);
  }

  @GetMapping("/{id}")
  public Pedido consultarPedido(@PathVariable String id) {
    return service.consultar(id);
  }

  @PostMapping("/{id}/aceptacion")
  public ResponseEntity<Pedido> aceptarPedido(@PathVariable String id) {
    var p = service.decidir(id, true);
    return ResponseEntity.status("CANCELADO".equals(p.estado) ? 409 : 200).body(p);
  }

  @PostMapping("/{id}/rechazo")
  public Pedido rechazarPedido(@PathVariable String id) {
    return service.decidir(id, false);
  }
}
