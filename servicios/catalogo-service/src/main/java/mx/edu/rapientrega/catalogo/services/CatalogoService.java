package mx.edu.rapientrega.catalogo.services;

import java.util.List;
import mx.edu.rapientrega.catalogo.dtos.*;
import mx.edu.rapientrega.catalogo.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CatalogoService {
  private final RestauranteRepository restaurantes;
  private final ProductoRepository productos;

  public CatalogoService(RestauranteRepository r, ProductoRepository p) {
    restaurantes = r;
    productos = p;
  }

  public List<RestauranteDto> consultarRestaurantes() {
    return restaurantes.findByDisponibleTrue().stream()
        .map(r -> new RestauranteDto(r.getId(), r.getNombre(), r.getLatitud(), r.getLongitud()))
        .toList();
  }

  public List<ProductoDto> consultarMenu(Long id) {
    if (!restaurantes.findById(id).filter(r -> r.isDisponible()).isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "RESTAURANTE_NO_DISPONIBLE");
    return productos.findByRestauranteIdAndDisponibleTrue(id).stream()
        .map(p -> new ProductoDto(p.getId(), p.getRestauranteId(), p.getNombre(), p.getPrecio()))
        .toList();
  }
}
