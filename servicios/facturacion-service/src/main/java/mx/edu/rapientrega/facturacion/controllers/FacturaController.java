package mx.edu.rapientrega.facturacion.controllers;

import mx.edu.rapientrega.facturacion.models.Factura;
import mx.edu.rapientrega.facturacion.services.DominioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/facturas")
public class FacturaController {
  private final DominioService s;

  public FacturaController(DominioService s) {
    this.s = s;
  }

  @GetMapping("/{id}")
  public Factura consultar(@PathVariable String id) {
    return s.consultar(id);
  }

  @PostMapping("/{id}/reintentar")
  public Factura recuperar(@PathVariable String id) {
    return s.recuperar(id);
  }
}
