#!/usr/bin/env bash
# D6 - the fake Luau script: a curl loop posing as a Roblox game server,
# posting one telemetry snapshot every 2 seconds with drifting numbers.
# This is the phase's definition-of-done tool, given complete (it is a
# harness, not a learning unit).
#
# Usage:
#   docker compose up -d && ./gradlew bootRun     (in one terminal)
#   ./scripts/fake-roblox.sh                      (in another)
#   then watch /topic/telemetry/8271 on any STOMP client.

HOST="${1:-http://localhost:8080}"
KEY="${SHAYVERI_KEY_ROBLOX:-dev-roblox-key}"
PLACE_ID="8271"
ROUND=1
TICK=0

echo "posing as Roblox place $PLACE_ID -> $HOST/api/telemetry (ctrl-C to stop)"

while true; do
  TICK=$((TICK + 1))
  # drifting fake numbers: players wander 8-24, fps wobbles around 55
  PLAYERS=$(( 8 + (TICK * 7) % 17 ))
  FPS=$(( 50 + (TICK * 3) % 11 ))
  # new round every 15 ticks
  if [ $((TICK % 15)) -eq 0 ]; then ROUND=$((ROUND + 1)); fi

  BODY=$(cat <<JSON
{"placeId":"$PLACE_ID","jobId":"job-fake-1","playerCount":$PLAYERS,"serverFps":$FPS.5,"round":"round-$ROUND","customMetrics":{"zombiesAlive":$((PLAYERS * 3))}}
JSON
)

  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$HOST/api/telemetry" \
    -H "X-Api-Key: $KEY" \
    -H "Content-Type: application/json" \
    -d "$BODY")

  echo "tick $TICK  players=$PLAYERS fps=$FPS.5 round=$ROUND  -> HTTP $STATUS"
  sleep 2
done
