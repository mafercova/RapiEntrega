package mx.edu.rapientrega.pagos.repositories;

import jakarta.persistence.LockModeType;
import java.util.*;
import mx.edu.rapientrega.pagos.models.Pago;
import org.springframework.data.jpa.repository.*;

public interface PagoRepository extends JpaRepository<Pago, String> {
  Optional<Pago> findByClave(String clave);

  Optional<Pago> findByPedidoId(String id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Pago p where p.id=:id")
  Optional<Pago> bloquear(String id);

  List<Pago> findByEstado(String estado);
}
