package dev.shayveri.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * B3 - turns on the STOMP broker (Phase-1-minimal slice of Module 6).
 *
 * Consumes (not ours):
 *   @EnableWebSocketMessageBroker - activates Spring's whole STOMP stack,
 *       including the SimpMessagingTemplate bean that B2 injects.
 *   WebSocketMessageBrokerConfigurer - the configuration interface; we
 *       override exactly two of its methods:
 *       registerStompEndpoints(StompEndpointRegistry) - WHERE clients connect
 *       configureMessageBroker(MessageBrokerRegistry) - WHAT destinations exist
 *
 * TODO(averi):
 *   1. registerStompEndpoints:
 *          registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
 *      "/ws" is the URL the dashboard's WebSocket handshake hits
 *      (ws://localhost:8080/ws). The origin pattern is dev-open for now;
 *      it tightens when the dashboard's real origin is known.
 *   2. configureMessageBroker:
 *          registry.enableSimpleBroker("/topic");
 *      = every destination starting /topic is handled by the in-memory
 *      broker (fine for two users, per the plan; the external-broker swap
 *      seam is exactly this line).
 *
 * SECURITY NOTE (per the plan's Module 6 design): handshake auth for the
 * DASH key in the CONNECT frame is deliberately NOT here yet - it comes
 * when Module 6 is built properly. Phase-1-minimal means open handshake;
 * leave the seam obvious.
 *
 * Done when: the app boots, and a raw STOMP client can connect to /ws and
 * subscribe to /topic/anything without error (D4 automates this).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// TODO(averi): step 1 above
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// TODO(averi): step 2 above
	}

}
