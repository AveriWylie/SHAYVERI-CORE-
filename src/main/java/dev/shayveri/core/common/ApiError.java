package dev.shayveri.core.common;

/**
 * C1 - the uniform error body every failed request returns.
 *
 * Responsibility: one fixed JSON shape for ALL errors, so every client
 * (Roblox scripts, node agents, dashboard) parses failures the same way.
 *
 * Consumes: nothing - this is entirely ours. Jackson serializes the record
 * to JSON automatically; each record component becomes a JSON key.
 *
 * A record is Java's immutable data carrier: you declare the components in
 * the header and the compiler generates the constructor, accessors,
 * equals/hashCode, and toString.
 *
 * TODO(averi): declare the four components in the parentheses below:
 *   int status                          - the HTTP status code (400, 500)
 *   String message                      - one human-readable summary line
 *   java.util.Map<String, String> fieldErrors
 *                                       - field name -> what is wrong with it
 *                                         (empty map when not a validation error)
 *   java.time.Instant timestamp         - when the error happened
 *
 * Done when: `new ApiError(400, "validation failed", Map.of("placeId",
 * "must not be blank"), Instant.now())` compiles, and C2 can build one.
 */
public record ApiError(
) {
}
