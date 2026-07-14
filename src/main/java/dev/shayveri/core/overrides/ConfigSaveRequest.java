package dev.shayveri.core.overrides;

import java.util.Map;

/**
 * O1 - PUT /api/config body. Consumes: Jakarta validation + Jackson.
 * TODO(averi): validation - placeId (nullable = global, no annotation), namespace @NotBlank,
 * values @NotNull Map. Compact constructor to default values to Map.of() if null.
 */
public record ConfigSaveRequest(
		String placeId,
		String namespace,
		Map<String, Object> values
) {
}
