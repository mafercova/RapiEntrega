package mx.edu.rapientrega.facturacion.repositories;

import mx.edu.rapientrega.facturacion.models.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxRepository extends JpaRepository<Inbox, String> {}
