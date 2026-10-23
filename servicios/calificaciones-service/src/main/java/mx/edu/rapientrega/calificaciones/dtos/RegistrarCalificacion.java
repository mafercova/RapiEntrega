package mx.edu.rapientrega.calificaciones.dtos;

import jakarta.validation.constraints.*;

public record RegistrarCalificacion(
    @NotBlank String pedidoId,
    @NotBlank String clienteId,
    @Min(1) @Max(5) int puntuacion,
    @Size(max = 500) String comentario) {}
