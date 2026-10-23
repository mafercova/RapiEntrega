package mx.edu.rapientrega.logistica.repositories;

import mx.edu.rapientrega.logistica.models.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxRepository extends JpaRepository<Inbox, String> {}
