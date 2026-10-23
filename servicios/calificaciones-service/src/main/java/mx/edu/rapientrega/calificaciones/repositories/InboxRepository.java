package mx.edu.rapientrega.calificaciones.repositories;

import mx.edu.rapientrega.calificaciones.models.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxRepository extends JpaRepository<Inbox, String> {}
