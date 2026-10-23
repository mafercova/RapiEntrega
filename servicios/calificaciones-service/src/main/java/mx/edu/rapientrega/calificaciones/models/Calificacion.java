package mx.edu.rapientrega.calificaciones.models;

import jakarta.persistence.*;

@Entity
public class Calificacion {
  @Id public String pedidoId;
  public String clienteId;
  public int puntuacion;
  public String comentario;

  protected Calificacion() {}

  public Calificacion(String p, String c, int n, String t) {
    pedidoId = p;
    clienteId = c;
    puntuacion = n;
    comentario = t;
  }
}
