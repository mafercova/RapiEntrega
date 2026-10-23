package mx.edu.rapientrega.pedidos.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Pedido {
  @Id public String id;
  @Version public long version;

  @Column(unique = true, nullable = false)
  public String clave;

  @Lob public String solicitud;
  public String clienteId;
  public Long restauranteId;
  public String estado;
  public BigDecimal total;
  public double latitud;
  public double longitud;
  public double recogidaLat;
  public double recogidaLon;
  public String direccion;
  public String pagoId;
  public String repartidorId;
  public Instant pendienteAceptacionAt;
  public Instant limite;
  @Lob public String partidas;

  protected Pedido() {}

  public Pedido(String id) {
    this.id = id;
  }
}
