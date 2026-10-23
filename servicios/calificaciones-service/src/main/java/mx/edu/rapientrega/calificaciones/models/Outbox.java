package mx.edu.rapientrega.calificaciones.models;

import jakarta.persistence.*;
import java.time.Instant;

/** Una entrega persistida por evento y suscriptor. */
@Entity
public class Outbox {
  @Id public String id;
  public String routingKey;
  public String destinatario;
  @Lob public String payload;
  public boolean enviado;
  public int intentos;
  public Instant proximoIntento;

  protected Outbox() {}

  public Outbox(String key, String json, String destino) {
    id = java.util.UUID.randomUUID().toString();
    routingKey = key;
    payload = json;
    destinatario = destino;
  }
}
