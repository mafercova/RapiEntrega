package mx.edu.rapientrega.catalogo.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Producto {
  @Id private Long id;
  private Long restauranteId;
  private String nombre;

  @Column(precision = 12, scale = 2)
  private BigDecimal precio;

  private boolean disponible;

  protected Producto() {}

  public Long getId() {
    return id;
  }

  public Long getRestauranteId() {
    return restauranteId;
  }

  public String getNombre() {
    return nombre;
  }

  public BigDecimal getPrecio() {
    return precio;
  }

  public boolean isDisponible() {
    return disponible;
  }
}
