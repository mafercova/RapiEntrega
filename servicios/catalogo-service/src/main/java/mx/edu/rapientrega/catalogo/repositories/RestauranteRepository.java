package mx.edu.rapientrega.catalogo.repositories;

import java.util.List;
import mx.edu.rapientrega.catalogo.models.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
  List<Restaurante> findByDisponibleTrue();
}
