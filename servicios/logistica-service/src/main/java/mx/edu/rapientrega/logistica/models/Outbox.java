package mx.edu.rapientrega.logistica.models;

import jakarta.persistence.*;

@Entity
public class Outbox {
  @Id public String id;
  public String routingKey;
  @Lob public String payload;
  public boolean enviado;

  protected Outbox() {}

  public Outbox(String key, String json) {
    id = java.util.UUID.randomUUID().toString();
    routingKey = key;
    payload = json;
  }
}
