package mx.edu.rapientrega.logistica.dtos;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record Ubicacion(
    @NotBlank String repartidorId,
    @DecimalMin("-90") @DecimalMax("90") double latitud,
    @DecimalMin("-180") @DecimalMax("180") double longitud,
    @NotNull Instant capturedAt,
    @Min(0) long sequence) {}
