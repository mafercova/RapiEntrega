package mx.edu.rapientrega.logistica.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Asignacion {
  @Id public String pedidoId;
  public String clienteId;
  public String repartidorId;
  public String estado;
  public double recogidaLat;
  public double recogidaLon;
  public double distanciaKm;
  public double latitud;
  public double longitud;
  public long secuencia = -1;
  public Instant capturedAt;
  public Instant receivedAt;

  protected Asignacion() {}

  public Asignacion(String p) {
    pedidoId = p;
  }
}
