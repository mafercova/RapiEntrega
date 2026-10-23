package mx.edu.rapientrega.catalogo.models;

import jakarta.persistence.*;

@Entity
public class Restaurante {
  @Id private Long id;
  private String nombre;
  private boolean disponible;
  private double latitud;
  private double longitud;

  protected Restaurante() {}

  public Long getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public boolean isDisponible() {
    return disponible;
  }

  public double getLatitud() {
    return latitud;
  }

  public double getLongitud() {
    return longitud;
  }
}
