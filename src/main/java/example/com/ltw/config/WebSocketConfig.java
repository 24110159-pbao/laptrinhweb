package example.com.ltw.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register /ws-chat with SockJS fallback for web browsers
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Register /ws-chat-raw for native WebSocket clients (e.g. tools/Postman)
        registry.addEndpoint("/ws-chat-raw")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Message broker prefixes for subscribed topics
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages sent from client to server @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
    }
}
