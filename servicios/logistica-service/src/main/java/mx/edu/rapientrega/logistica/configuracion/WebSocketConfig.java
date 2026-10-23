package mx.edu.rapientrega.logistica.configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  public void registerStompEndpoints(StompEndpointRegistry r) {
    r.addEndpoint("/ws");
  }

  public void configureMessageBroker(MessageBrokerRegistry r) {
    r.enableSimpleBroker("/topic");
    r.setApplicationDestinationPrefixes("/app");
  }
}
