package mx.edu.rapientrega.pedidos.repositories;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import mx.edu.rapientrega.pedidos.models.Pedido;
import org.springframework.data.jpa.repository.*;

public interface PedidoRepository extends JpaRepository<Pedido, String> {
  Optional<Pedido> findByClave(String clave);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Pedido p where p.id=:id")
  Optional<Pedido> bloquear(String id);

  List<Pedido> findByEstadoAndLimiteLessThanEqual(String estado, Instant now);
}
