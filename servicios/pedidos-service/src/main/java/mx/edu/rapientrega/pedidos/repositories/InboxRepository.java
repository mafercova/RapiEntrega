package mx.edu.rapientrega.pedidos.repositories;

import mx.edu.rapientrega.pedidos.models.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxRepository extends JpaRepository<Inbox, String> {}
