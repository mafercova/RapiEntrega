package mx.edu.rapientrega.facturacion.services;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LegadoAdapter {
  private final RestClient h;
  private final String url;

  public LegadoAdapter(RestClient h, @Value("${app.legado-url}") String u) {
    this.h = h;
    url = u;
  }

  public String emitir(String pedido, String cliente) {
    var r =
        h.post()
            .uri(url + "/legacy2012/comprobantes")
            .header("Idempotency-Key", "invoice-" + pedido)
            .body(Map.of("ID_ORDEN", pedido, "ID_CLIENTE", cliente, "VERSION_FORMATO", 2012))
            .retrieve()
            .body(Map.class);
    return r.get("NUM_FOLIO").toString();
  }
}
