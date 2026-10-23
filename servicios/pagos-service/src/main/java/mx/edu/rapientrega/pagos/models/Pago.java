package mx.edu.rapientrega.pagos.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Pago {
  @Id public String id;
  @Version public long version;

  @Column(unique = true)
  public String pedidoId;

  @Column(unique = true)
  public String clave;

  public String fingerprint;
  public BigDecimal importe;
  public String estado;
  public String referencia;
  public String reembolsoReferencia;
  public int reembolsoIntentos;
  public java.time.Instant proximoReintento;

  protected Pago() {}

  public Pago(String id) {
    this.id = id;
  }
}
