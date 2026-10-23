package mx.edu.rapientrega.facturacion.repositories;

import jakarta.persistence.LockModeType;
import java.util.*;
import mx.edu.rapientrega.facturacion.models.Factura;
import org.springframework.data.jpa.repository.*;

public interface FacturaRepository extends JpaRepository<Factura, String> {
  List<Factura> findByEstado(String e);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select f from Factura f where f.pedidoId=:id")
  Optional<Factura> bloquear(String id);
}
