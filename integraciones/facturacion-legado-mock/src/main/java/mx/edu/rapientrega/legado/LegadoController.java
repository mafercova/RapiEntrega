package mx.edu.rapientrega.legado;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class LegadoController {
  private final Map<String, Map<String, Object>> folios = new HashMap<>();
  private final Map<String, Integer> intentos = new HashMap<>();

  @PostMapping("/legacy2012/comprobantes")
  public synchronized Map<String, Object> emitir(
      @RequestHeader("Idempotency-Key") String key,
      @RequestHeader(value = "X-Mock-Failures", defaultValue = "0") int n,
      @RequestBody Map<String, Object> body) {
    if (intentos.merge(key, 1, Integer::sum) <= Math.min(n, 5))
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "LEGADO_SIMULADO_NO_DISPONIBLE");
    return folios.computeIfAbsent(
        key, k -> Map.of("NUM_FOLIO", "L2012-" + UUID.randomUUID(), "SIMULACION", true));
  }
}
