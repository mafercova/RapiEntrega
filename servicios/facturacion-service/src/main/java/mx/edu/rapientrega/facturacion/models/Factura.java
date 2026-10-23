package mx.edu.rapientrega.facturacion.models;

import jakarta.persistence.*;

@Entity
public class Factura {
  @Id public String pedidoId;
  public String clienteId;
  public String estado;
  public String folio;
  public int intentos;
  public java.time.Instant proximoReintento;

  protected Factura() {}

  public Factura(String p, String c) {
    pedidoId = p;
    clienteId = c;
    estado = "PENDIENTE";
  }
}
