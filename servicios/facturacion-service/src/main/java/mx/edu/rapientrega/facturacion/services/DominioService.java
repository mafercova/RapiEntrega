package mx.edu.rapientrega.facturacion.services;

import java.time.Clock;
import mx.edu.rapientrega.facturacion.dtos.Evento;
import mx.edu.rapientrega.facturacion.models.*;
import mx.edu.rapientrega.facturacion.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DominioService {
  private final FacturaRepository repo;
  private final InboxRepository inbox;
  private final LegadoAdapter legado;
  private final Clock clock;

  public DominioService(FacturaRepository r, InboxRepository i, LegadoAdapter l, Clock c) {
    repo = r;
    inbox = i;
    legado = l;
    clock = c;
  }

  @Transactional
  public void consumir(Evento e) {
    if (inbox.existsById(e.eventId())) return;
    if (!repo.existsById(e.aggregateId())) {
      var f = new Factura(e.aggregateId(), e.payload().get("clienteId").toString());
      f.proximoReintento = clock.instant();
      repo.save(f);
    }
    inbox.save(new Inbox(e.eventId()));
  }

  @Transactional
  public void emitirPendientes() {
    for (var row : repo.findByEstado("PENDIENTE")) {
      var f = repo.bloquear(row.pedidoId).orElseThrow();
      if (!"PENDIENTE".equals(f.estado) || f.proximoReintento.isAfter(clock.instant())) continue;
      try {
        f.folio = legado.emitir(f.pedidoId, f.clienteId);
        f.estado = "EMITIDA";
      } catch (org.springframework.web.client.RestClientException e) {
        f.intentos++;
        if (f.intentos >= 3) f.estado = "REVISION";
        else f.proximoReintento = clock.instant().plusSeconds(1L << f.intentos);
      }
    }
  }

  public Factura consultar(String id) {
    return repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FACTURA_NO_EXISTE"));
  }

  @Transactional
  public Factura recuperar(String id) {
    var f =
        repo.bloquear(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FACTURA_NO_EXISTE"));
    if (!"REVISION".equals(f.estado))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "NO_REQUIERE_RECUPERACION");
    f.estado = "PENDIENTE";
    f.intentos = 0;
    f.proximoReintento = clock.instant();
    return f;
  }
}
