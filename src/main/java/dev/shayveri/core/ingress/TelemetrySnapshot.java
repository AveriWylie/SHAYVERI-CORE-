package dev.shayveri.core.ingress;

/**
 * A3 - the persisted form of a telemetry snapshot: everything A1 carried,
 * plus the server-side receivedAt stamp. Documents are classes (not
 * records) here: Spring Data Mongo works best with a mutable class it can
 * instantiate and populate when reading back.
 *
 * Consumes (not ours) - all from Spring Data MongoDB:
 *   @Document("telemetry_snapshots")
 *              - class-level: "instances of this class live in the Mongo
 *                collection named telemetry_snapshots."
 *                (import org.springframework.data.mongodb.core.mapping.Document)
 *   @Id        - marks the identifier field; leave it null on insert and
 *                Mongo generates a unique id, Spring writes it back.
 *                (import org.springframework.data.annotation.Id)
 *   @Indexed(expireAfter = "7d")
 *              - THE TTL: put this on the receivedAt field and Mongo itself
 *                deletes documents 7 days after that instant. No cleanup
 *                job, no scheduler - the database does it. This is the
 *                plan's "raw snapshots are disposable".
 *                (import org.springframework.data.mongodb.core.index.Indexed)
 *
 * IMPORTANT (blueprint A3 note): index creation from annotations is OFF by
 * default in Spring Boot. Add to application.yml:
 *   spring.data.mongodb.auto-index-creation: true
 * so a fresh database gets the TTL index automatically. Without it the
 * annotation is decoration and nothing ever expires.
 *
 * TODO(averi): build the class:
 *   1. @Document annotation on the class.
 *   2. Fields: @Id String id;
 *              String placeId; String jobId; Integer playerCount;
 *              Double serverFps; String round;
 *              Map<String, Object> customMetrics;
 *              @Indexed(expireAfter = "7d") Instant receivedAt;
 *   3. A constructor taking everything except id (id stays null until
 *      Mongo assigns it), and getters. (Setters optional - Spring Data can
 *      populate final-less fields via reflection; keep it simple: private
 *      fields, one constructor, getters.)
 *   4. A static factory tying the layers together:
 *        static TelemetrySnapshot from(TelemetrySnapshotRequest req, Instant receivedAt)
 *      so the service (A7) converts request -> document in one call and
 *      the conversion logic has exactly one home.
 *
 * Done when: D3 shows an accepted snapshot in the collection with
 * receivedAt set, and the TTL index exists with 7-day expiry.
 */
public class TelemetrySnapshot {

}
