package mx.edu.rapientrega.pagos.models;

import jakarta.persistence.*;

@Entity
public class Inbox {
  @Id public String id;

  protected Inbox() {}

  public Inbox(String id) {
    this.id = id;
  }
}
