package com.porterclone.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket for live trip tracking.
 * Customer app subscribes to /topic/trip/{tripId}/location and gets pushed updates
 * whenever the assigned rider's location changes (see LocationBroadcastService).
 *
 * Scaling note: with multiple app instances behind a load balancer, replace the
 * simple in-memory broker below with a Redis- or RabbitMQ-backed STOMP relay so a
 * customer connected to instance A still receives updates published from instance B.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String frontendOrigin;

    public WebSocketConfig(@Value("${app.cors.allowed-origin:http://localhost:5173}") String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(frontendOrigin).withSockJS();
    }
}
