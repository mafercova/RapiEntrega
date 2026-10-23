package mx.edu.rapientrega.pagos.configuracion;

import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name = "app.messaging", havingValue = "true", matchIfMissing = true)
public class Messaging {
  @Bean
  Declarables topology() {
    var ex = new TopicExchange("rapientrega.eventos", true, false);
    var dlx = new DirectExchange("rapientrega.dlx", true, false);
    var all = new java.util.ArrayList<Declarable>();
    all.add(ex);
    all.add(dlx);
    {
      var q =
          QueueBuilder.durable("pedidos.eventos")
              .deadLetterExchange("rapientrega.dlx")
              .deadLetterRoutingKey("pedidos")
              .build();
      var dlq = new Queue("pedidos.dlq", true);
      all.add(q);
      all.add(dlq);
      all.add(BindingBuilder.bind(dlq).to(dlx).with("pedidos"));
      all.add(BindingBuilder.bind(q).to(ex).with("pago.confirmado.v1"));
      all.add(BindingBuilder.bind(q).to(ex).with("pago.rechazado.v1"));
      all.add(BindingBuilder.bind(q).to(ex).with("repartidor.asignado.v1"));
      all.add(BindingBuilder.bind(q).to(ex).with("asignacion.pendiente.v1"));
      all.add(BindingBuilder.bind(q).to(ex).with("pedido.entregado.v1"));
    }
    {
      var q =
          QueueBuilder.durable("pagos.eventos")
              .deadLetterExchange("rapientrega.dlx")
              .deadLetterRoutingKey("pagos")
              .build();
      var dlq = new Queue("pagos.dlq", true);
      all.add(q);
      all.add(dlq);
      all.add(BindingBuilder.bind(dlq).to(dlx).with("pagos"));
      all.add(BindingBuilder.bind(q).to(ex).with("pedido.cancelado.v1"));
    }
    {
      var q =
          QueueBuilder.durable("logistica.eventos")
              .deadLetterExchange("rapientrega.dlx")
              .deadLetterRoutingKey("logistica")
              .build();
      var dlq = new Queue("logistica.dlq", true);
      all.add(q);
      all.add(dlq);
      all.add(BindingBuilder.bind(dlq).to(dlx).with("logistica"));
      all.add(BindingBuilder.bind(q).to(ex).with("pedido.aceptado.v1"));
    }
    {
      var q =
          QueueBuilder.durable("facturacion.eventos")
              .deadLetterExchange("rapientrega.dlx")
              .deadLetterRoutingKey("facturacion")
              .build();
      var dlq = new Queue("facturacion.dlq", true);
      all.add(q);
      all.add(dlq);
      all.add(BindingBuilder.bind(dlq).to(dlx).with("facturacion"));
      all.add(BindingBuilder.bind(q).to(ex).with("pedido.entregado.v1"));
    }
    {
      var q =
          QueueBuilder.durable("calificaciones.eventos")
              .deadLetterExchange("rapientrega.dlx")
              .deadLetterRoutingKey("calificaciones")
              .build();
      var dlq = new Queue("calificaciones.dlq", true);
      all.add(q);
      all.add(dlq);
      all.add(BindingBuilder.bind(dlq).to(dlx).with("calificaciones"));
      all.add(BindingBuilder.bind(q).to(ex).with("pedido.entregado.v1"));
    }
    return new Declarables(all);
  }
}
