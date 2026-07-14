package dev.shayveri.core.ingress;

/**
 * A4 - the persisted form of a game event. This collection is the research
 * dataset (heatmaps, balance analysis), so unlike A3 there is NO TTL -
 * nothing here ever expires.
 *
 * Consumes: @Document("game_events") and @Id, exactly as in A3. No
 * @Indexed(expireAfter) - the absence IS the design decision. (A plain
 * @Indexed on placeId is worth adding: heatmap queries will filter by it.)
 *
 * TODO: mirror what you did in A3:
 *   1. @Document("game_events") on the class.
 *   2. Fields: @Id String id;
 *              String type; @Indexed String placeId; String jobId;
 *              Instant occurredAt;              (client clock)
 *              GameEventRequest.Position position;  (nullable)
 *              Map<String, Object> data;
 *              Instant receivedAt;              (server clock - both stored
 *                                                on purpose: client clocks
 *                                                drift, and the gap itself
 *                                                is diagnostic data)
 *   3. Constructor (all but id) + getters.
 *   4. Static factory: from(GameEventRequest req, Instant receivedAt).
 *
 * Done when: D3 shows a batch of 3 posted events -> 3 documents here, and
 * the collection has no TTL index.
 */
public class GameEvent {

}
