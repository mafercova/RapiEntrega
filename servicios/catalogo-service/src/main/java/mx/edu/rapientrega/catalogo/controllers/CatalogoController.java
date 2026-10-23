package mx.edu.rapientrega.catalogo.controllers;

import jakarta.validation.constraints.Positive;
import java.util.List;
import mx.edu.rapientrega.catalogo.dtos.*;
import mx.edu.rapientrega.catalogo.services.CatalogoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/restaurantes")
@Validated
public class CatalogoController {
  private final CatalogoService service;

  public CatalogoController(CatalogoService s) {
    service = s;
  }

  @GetMapping
  public List<RestauranteDto> consultarRestaurantes() {
    return service.consultarRestaurantes();
  }

  @GetMapping("/{id}/menu")
  public List<ProductoDto> consultarMenu(@PathVariable @Positive Long id) {
    return service.consultarMenu(id);
  }
}
