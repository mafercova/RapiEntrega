package mx.edu.rapientrega.pedidos.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record RegistrarPedido(
    @NotBlank String clienteId,
    @NotNull @Positive Long restauranteId,
    @NotEmpty @Size(max = 100) List<@Valid Item> items,
    @Valid @NotNull Entrega entrega) {
  public record Item(@NotNull @Positive Long productoId, @Min(1) @Max(99) int cantidad) {}

  public record Entrega(
      @DecimalMin("-90") @DecimalMax("90") double latitud,
      @DecimalMin("-180") @DecimalMax("180") double longitud,
      @NotBlank @Size(max = 300) String direccion) {}
}
