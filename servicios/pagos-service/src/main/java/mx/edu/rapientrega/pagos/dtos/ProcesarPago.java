package mx.edu.rapientrega.pagos.dtos;

import jakarta.validation.constraints.*;

public record ProcesarPago(@NotBlank String pedidoId, @NotBlank String tokenTarjeta) {}
