package mx.edu.rapientrega.catalogo.repositories;

import java.util.List;
import mx.edu.rapientrega.catalogo.models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
  List<Producto> findByRestauranteIdAndDisponibleTrue(Long restauranteId);
}
