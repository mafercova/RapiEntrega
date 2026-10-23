package mx.edu.rapientrega.pasarela;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/mock")
public class PasarelaController {
  public record Solicitud(String pedidoId, BigDecimal importe, String tokenTarjeta) {}

  public record Resultado(String referencia, String estado, boolean simulacion) {}

  private final Map<String, Resultado> operaciones = new HashMap<>();
  private final Map<String, Solicitud> cuerpos = new HashMap<>();
  private final Map<String, Integer> intentos = new HashMap<>();
  private final Set<String> timeout = new HashSet<>();

  private void fallar(String key, int n) {
    int i = intentos.merge(key, 1, Integer::sum);
    if (i <= Math.min(n, 5))
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FALLO_SIMULADO");
  }

  @PostMapping("/cobros")
  public synchronized Resultado cobrar(
      @RequestHeader("Idempotency-Key") String key,
      @RequestHeader(value = "X-Mock-Failures", defaultValue = "0") int fallos,
      @RequestBody Solicitud s) {
    fallar(key, fallos);
    if (cuerpos.containsKey(key) && !cuerpos.get(key).equals(s))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "CLAVE_REUTILIZADA");
    cuerpos.putIfAbsent(key, s);
    if (s.importe() == null || s.importe().signum() <= 0 || s.tokenTarjeta() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    var result =
        operaciones.computeIfAbsent(
            key,
            k ->
                new Resultado(
                    UUID.randomUUID().toString(),
                    "reject".equals(s.tokenTarjeta()) ? "RECHAZADO" : "AUTORIZADO",
                    true));
    if ("timeout-once".equals(s.tokenTarjeta()) && timeout.add(key))
      throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "RESPUESTA_PERDIDA_SIMULADA");
    return result;
  }

  @GetMapping("/operaciones/{key}")
  public synchronized Resultado consultar(@PathVariable String key) {
    var r = operaciones.get(key);
    if (r == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return r;
  }

  @PostMapping("/reembolsos/{pagoId}")
  public synchronized Resultado reembolsar(
      @PathVariable String pagoId,
      @RequestHeader("Idempotency-Key") String key,
      @RequestHeader(value = "X-Mock-Failures", defaultValue = "0") int fallos) {
    fallar(key, fallos);
    return operaciones.computeIfAbsent(
        key, k -> new Resultado("refund-" + pagoId, "REEMBOLSADO", true));
  }
}
