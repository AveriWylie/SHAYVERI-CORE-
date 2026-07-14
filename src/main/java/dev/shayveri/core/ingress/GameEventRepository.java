package dev.shayveri.core.ingress;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Consumed-API declaration for A6, same story as TelemetrySnapshotRepository:
 * Spring Data generates the implementation. saveAll(List) is the call A6
 * will want for event batches. Package-private on purpose.
 */
interface GameEventRepository extends MongoRepository<GameEvent, String> {
}
