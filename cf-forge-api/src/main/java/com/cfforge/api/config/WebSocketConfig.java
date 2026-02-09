package com.cfforge.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Collaboration endpoint with SockJS fallback
        registry.addEndpoint("/ws/collab")
            .setAllowedOriginPatterns("*")
            .withSockJS();

        // Shared terminal sessions
        registry.addEndpoint("/ws/terminal")
            .setAllowedOriginPatterns("*")
            .withSockJS();

        // Build/deploy log streaming
        registry.addEndpoint("/ws/logs")
            .setAllowedOriginPatterns("*");

        // Agent conversation streaming
        registry.addEndpoint("/ws/agent")
            .setAllowedOriginPatterns("*");

        // Build progress streaming
        registry.addEndpoint("/ws/build")
            .setAllowedOriginPatterns("*");
    }
}
