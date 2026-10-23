package mx.edu.rapientrega.logistica.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  @Value("${app.websocket-relay:false}")
  boolean relay;

  @Value("${spring.rabbitmq.host}")
  String host;

  @Value("${spring.rabbitmq.username}")
  String username;

  @Value("${spring.rabbitmq.password}")
  String password;

  public void registerStompEndpoints(StompEndpointRegistry r) {
    r.addEndpoint("/ws");
  }

  public void configureMessageBroker(MessageBrokerRegistry r) {
    if (relay)
      r.enableStompBrokerRelay("/topic")
          .setRelayHost(host)
          .setRelayPort(61613)
          .setClientLogin(username)
          .setClientPasscode(password)
          .setSystemLogin(username)
          .setSystemPasscode(password);
    else r.enableSimpleBroker("/topic");
    r.setApplicationDestinationPrefixes("/app");
  }
}
