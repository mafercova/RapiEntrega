package mx.edu.rapientrega.logistica.configuracion;

import mx.edu.rapientrega.logistica.models.Repartidor;
import mx.edu.rapientrega.logistica.repositories.RepartidorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
@Profile("demo")
public class DatosDemo {
  @Bean
  CommandLineRunner seed(RepartidorRepository r) {
    return args -> {
      if (r.count() == 0)
        r.saveAll(
            java.util.List.of(
                new Repartidor("00000000-0000-0000-0000-000000000001", 19.4327, -99.1333),
                new Repartidor("00000000-0000-0000-0000-000000000002", 19.50, -99.20)));
    };
  }
}
