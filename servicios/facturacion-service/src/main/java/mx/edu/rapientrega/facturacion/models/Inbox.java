package mx.edu.rapientrega.facturacion.models;

import jakarta.persistence.*;

@Entity
public class Inbox {
  @Id public String id;

  protected Inbox() {}

  public Inbox(String id) {
    this.id = id;
  }
}
