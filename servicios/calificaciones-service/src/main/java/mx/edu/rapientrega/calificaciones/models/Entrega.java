package mx.edu.rapientrega.calificaciones.models;

import jakarta.persistence.*;

@Entity
public class Entrega {
  @Id public String pedidoId;
  public String clienteId;

  protected Entrega() {}

  public Entrega(String p, String c) {
    pedidoId = p;
    clienteId = c;
  }
}
