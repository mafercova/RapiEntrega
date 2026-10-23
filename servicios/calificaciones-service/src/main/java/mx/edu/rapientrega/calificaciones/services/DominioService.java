package mx.edu.rapientrega.calificaciones.services;

import java.util.Objects;
import mx.edu.rapientrega.calificaciones.dtos.*;
import mx.edu.rapientrega.calificaciones.models.*;
import mx.edu.rapientrega.calificaciones.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DominioService {
  private final EntregaRepository entregas;
  private final CalificacionRepository repo;
  private final InboxRepository inbox;

  public DominioService(EntregaRepository e, CalificacionRepository r, InboxRepository i) {
    entregas = e;
    repo = r;
    inbox = i;
  }

  @Transactional
  public void consumir(Evento e) {
    if (inbox.existsById(e.eventId())) return;
    entregas.save(new Entrega(e.aggregateId(), e.payload().get("clienteId").toString()));
    inbox.save(new Inbox(e.eventId()));
  }

  @Transactional
  public Calificacion registrarCalificacion(RegistrarCalificacion s) {
    var e =
        entregas
            .findById(s.pedidoId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "PEDIDO_NO_ENTREGADO_O_EVENTO_PENDIENTE"));
    if (!e.clienteId.equals(s.clienteId()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLIENTE_NO_CORRESPONDE");
    var old = repo.findById(s.pedidoId());
    if (old.isPresent()) {
      var c = old.get();
      if (c.puntuacion != s.puntuacion() || !Objects.equals(c.comentario, s.comentario()))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "CALIFICACION_YA_REGISTRADA");
      return c;
    }
    return repo.save(new Calificacion(s.pedidoId(), s.clienteId(), s.puntuacion(), s.comentario()));
  }

  public Calificacion consultar(String id) {
    return repo.findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CALIFICACION_NO_EXISTE"));
  }
}
