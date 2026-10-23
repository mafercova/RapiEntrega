package mx.edu.rapientrega.calificaciones.controllers;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class Errores {
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ProblemDetail> negocio(ResponseStatusException e) {
    return error(e.getStatusCode().value(), e.getReason());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    ConstraintViolationException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  ResponseEntity<ProblemDetail> datos(Exception e) {
    return error(400, "DATOS_INVALIDOS");
  }

  @ExceptionHandler(RestClientException.class)
  ResponseEntity<ProblemDetail> dependencia(Exception e) {
    return error(503, "DEPENDENCIA_NO_DISPONIBLE");
  }

  @ExceptionHandler({
    org.springframework.dao.ConcurrencyFailureException.class,
    org.springframework.dao.DataIntegrityViolationException.class
  })
  ResponseEntity<ProblemDetail> conflicto(Exception e) {
    return error(409, "CONFLICTO_CONCURRENCIA_REINTENTAR");
  }

  private ResponseEntity<ProblemDetail> error(int s, String c) {
    var p = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(s), c == null ? "ERROR" : c);
    p.setProperty("code", c);
    p.setProperty("correlationId", java.util.UUID.randomUUID().toString());
    return ResponseEntity.status(s).body(p);
  }
}
