package mx.edu.rapientrega.logistica.models;

import jakarta.persistence.*;

@Entity
public class Repartidor {
  @Id public String id;
  public double latitud;
  public double longitud;
  public boolean disponible;

  protected Repartidor() {}

  public Repartidor(String id, double lat, double lon) {
    this.id = id;
    latitud = lat;
    longitud = lon;
    disponible = true;
  }
}
