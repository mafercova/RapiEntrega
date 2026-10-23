package mx.edu.rapientrega.pagos.configuracion;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class Infrastructure {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  RestClient restClient() {
    var f =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    f.setReadTimeout(Duration.ofSeconds(3));
    return RestClient.builder().requestFactory(f).build();
  }
}
