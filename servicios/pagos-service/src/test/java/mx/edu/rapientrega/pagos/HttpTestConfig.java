package mx.edu.rapientrega.pagos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class HttpTestConfig {
  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

  @Bean
  @Primary
  RestClient testHttp() {
    return builder.build();
  }

  @Bean
  MockRestServiceServer server() {
    return server;
  }
}
