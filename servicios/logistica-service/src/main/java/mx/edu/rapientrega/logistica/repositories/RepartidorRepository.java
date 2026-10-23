package mx.edu.rapientrega.logistica.repositories;

import jakarta.persistence.LockModeType;
import java.util.*;
import mx.edu.rapientrega.logistica.models.Repartidor;
import org.springframework.data.jpa.repository.*;

public interface RepartidorRepository extends JpaRepository<Repartidor, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Repartidor r where r.disponible=true order by r.id")
  List<Repartidor> disponibles();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Repartidor r where r.id=:id")
  Optional<Repartidor> bloquear(String id);
}
