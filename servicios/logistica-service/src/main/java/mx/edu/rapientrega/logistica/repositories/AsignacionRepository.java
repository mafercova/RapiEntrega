package mx.edu.rapientrega.logistica.repositories;

import jakarta.persistence.LockModeType;
import java.util.*;
import mx.edu.rapientrega.logistica.models.Asignacion;
import org.springframework.data.jpa.repository.*;

public interface AsignacionRepository extends JpaRepository<Asignacion, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Asignacion a where a.pedidoId=:id")
  Optional<Asignacion> bloquear(String id);

  List<Asignacion> findByEstado(String estado);
}
