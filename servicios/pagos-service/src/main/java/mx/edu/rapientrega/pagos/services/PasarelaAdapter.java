package mx.edu.rapientrega.pagos.services;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class PasarelaAdapter {
  private final RestClient http;
  private final String url;

  public record Resultado(String referencia, String estado, boolean simulacion) {}

  public PasarelaAdapter(RestClient h, @Value("${app.pasarela-url}") String u) {
    http = h;
    url = u;
  }

  public Resultado cobrar(String clave, String pedido, BigDecimal total, String token) {
    try {
      return http.post()
          .uri(url + "/mock/cobros")
          .header("Idempotency-Key", clave)
          .body(Map.of("pedidoId", pedido, "importe", total, "tokenTarjeta", token))
          .retrieve()
          .body(Resultado.class);
    } catch (RestClientException ex) {
      try {
        return http.get()
            .uri(url + "/mock/operaciones/{key}", clave)
            .retrieve()
            .body(Resultado.class);
      } catch (RestClientException ignored) {
        throw ex;
      }
    }
  }

  public Resultado reembolsar(String pago) {
    return http.post()
        .uri(url + "/mock/reembolsos/{id}", pago)
        .header("Idempotency-Key", "refund-" + pago)
        .retrieve()
        .body(Resultado.class);
  }
}
