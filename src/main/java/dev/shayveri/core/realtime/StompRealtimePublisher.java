package dev.shayveri.core.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * B2 - the STOMP adapter behind B1.
 *
 * Consumes (not ours):
 *   SimpMessagingTemplate - Spring's one entry point for STOMP broadcast.
 *       The bean exists because B3 turns on @EnableWebSocketMessageBroker.
 *       The one method that matters:
 *           convertAndSend(String destination, Object payload)
 *       "convert" = Jackson serializes payload to JSON; "send" = every
 *       client subscribed to that destination receives it.
 *   @Component - Spring DI, as in A6.
 *
 * TODO(averi):
 *   1. private final field + constructor for the SimpMessagingTemplate.
 *   2. publish: one line - convertAndSend(topic, payload).
 *
 * Done when: D4 passes - a subscribed test client receives what was
 * published, and only on the topic it subscribed to.
 */
@Component
public class StompRealtimePublisher implements RealtimePublisher {

	@Override
	public void publish(String topic, Object payload) {
		throw new UnsupportedOperationException("TODO(averi): implement per B2");
	}

}
