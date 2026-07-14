package dev.shayveri.core.ingress;

import java.util.Map;

/**
 * A1 - the exact shape of POST /api/telemetry's JSON body. This IS the
 * contract with the Roblox-side Luau script; the plan fixes these names.
 *
 * The components are declared (the contract is given); what is left to you
 * is the VALIDATION - the annotations that make bad payloads impossible to
 * miss. Validation lives here as annotations and nowhere else.
 *
 * Consumes (not ours):
 *   Jackson              - binds incoming JSON to this record automatically;
 *                          each JSON key maps to the same-named component.
 *                          Invisible: no annotation needed when names match.
 *   Jakarta Validation   - the annotations below, package jakarta.validation
 *                          .constraints.*. They do nothing on their own; they
 *                          fire when the controller (A8) marks the parameter
 *                          @Valid, and failures surface as
 *                          MethodArgumentNotValidException -> handled by C2.
 *
 * TODO(averi): add validation annotations directly above each component:
 *   placeId       @NotBlank          (import jakarta.validation.constraints.NotBlank)
 *                                    - null, "", and "   " all rejected
 *   jobId         @NotBlank
 *   playerCount   @NotNull @Min(0)   - required, and never negative
 *   serverFps     @NotNull @Positive - required, and zero/negative is nonsense
 *   round         (no annotation - free-form, optional)
 *   customMetrics (no annotation - optional)
 *
 * TODO(averi, second): add a COMPACT CONSTRUCTOR to default customMetrics
 * to an empty map so no downstream code ever null-checks it:
 *
 *   public TelemetrySnapshotRequest {
 *       customMetrics = (customMetrics == null) ? Map.of() : customMetrics;
 *   }
 *
 * (A compact constructor is record syntax: no parentheses, runs before the
 * components are assigned - the place for normalization like this.)
 *
 * Done when: the D1 validation tests pass - a missing placeId produces a
 * violation naming "placeId", a valid body produces zero violations.
 */
public record TelemetrySnapshotRequest(
		String placeId,
		String jobId,
		Integer playerCount,
		Double serverFps,
		String round,
		Map<String, Object> customMetrics)
	{
}
