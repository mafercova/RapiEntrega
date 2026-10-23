package mx.edu.rapientrega.pagos.controllers;

import jakarta.validation.Valid;
import mx.edu.rapientrega.pagos.dtos.ProcesarPago;
import mx.edu.rapientrega.pagos.models.Pago;
import mx.edu.rapientrega.pagos.services.DominioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {
  private final DominioService s;

  public PagoController(DominioService s) {
    this.s = s;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Pago procesarPago(
      @RequestHeader("Idempotency-Key") String k, @Valid @RequestBody ProcesarPago p) {
    return s.procesarPago(k, p);
  }

  @GetMapping("/pedido/{id}")
  public Pago consultar(@PathVariable String id) {
    return s.consultar(id);
  }

  @PostMapping("/pedido/{id}/reintentar-reembolso")
  public Pago recuperar(@PathVariable String id) {
    return s.recuperarReembolso(id);
  }
}
