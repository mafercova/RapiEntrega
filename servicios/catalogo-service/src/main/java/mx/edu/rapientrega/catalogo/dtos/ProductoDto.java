package mx.edu.rapientrega.catalogo.dtos;

import java.math.BigDecimal;

public record ProductoDto(Long id, Long restauranteId, String nombre, BigDecimal precio) {}
