package mx.edu.rapientrega.pagos.repositories;

import jakarta.persistence.LockModeType;
import java.util.List;
import mx.edu.rapientrega.pagos.models.Outbox;
import org.springframework.data.jpa.repository.*;

public interface OutboxRepository extends JpaRepository<Outbox, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Outbox o where o.enviado=false order by o.id")
  List<Outbox> pendientes();
}
