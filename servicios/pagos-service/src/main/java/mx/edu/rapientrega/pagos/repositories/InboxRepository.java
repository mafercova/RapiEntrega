package mx.edu.rapientrega.pagos.repositories;

import mx.edu.rapientrega.pagos.models.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxRepository extends JpaRepository<Inbox, String> {}
