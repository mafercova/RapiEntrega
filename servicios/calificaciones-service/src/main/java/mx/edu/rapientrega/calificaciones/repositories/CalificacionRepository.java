package mx.edu.rapientrega.calificaciones.repositories;

import mx.edu.rapientrega.calificaciones.models.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalificacionRepository extends JpaRepository<Calificacion, String> {}
