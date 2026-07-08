# SHAYVERI CORE

**Compute Orchestration & Rendering Engine** - the Java backend for the SHAYVERI Roblox pipeline.

CORE is the single source of truth for the whole system. Roblox game servers and lab compute nodes are *clients* of CORE: they push data in and poll/receive config out. The React dashboard is a read/write control surface over WebSocket + REST. CORE is the only component that talks to the Roblox Open Cloud API.

---

## Table of Contents

- [System Context](#system-context)
- [Non-Goals (v1)](#non-goals-v1)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Project Layout](#project-layout)
- [Security Model](#security-model)
- [Configuration](#configuration)
- [Module Plan](#module-plan)
  - [Module 1 - Ingress (Telemetry Intake)](#module-1---ingress-telemetry-intake)
  - [Module 2 - Nodes (Lab Compute Registry)](#module-2---nodes-lab-compute-registry)
  - [Module 3 - Jobs (Pipeline Queue & Lifecycle)](#module-3---jobs-pipeline-queue--lifecycle)
  - [Module 4 - Overrides (Game Config Control Plane)](#module-4---overrides-game-config-control-plane)
  - [Module 5 - Egress (Roblox Open Cloud Client)](#module-5---egress-roblox-open-cloud-client)
  - [Module 6 - Realtime (Dashboard WebSocket Hub)](#module-6---realtime-dashboard-websocket-hub)
  - [Module 7 - Observability & Audit](#module-7---observability--audit)
- [Build Order & Milestones](#build-order--milestones)
- [Development Rules](#development-rules)
- [Deferred / v2 Ideas](#deferred--v2-ideas)

---

## System Context

```
[ Roblox Game Servers ] --HTTP--> ┌──────────────────────┐ --WebSocket--> [ React Dashboard ]
[ Lab Compute Nodes   ] --HTTP--> │    SHAYVERI CORE     │
                                  │  (Java / Spring Boot)│ --HTTPS--> [ Roblox Open Cloud API ]
                                  └──────────┬───────────┘
                                             │
                                  [ MongoDB ]  [ Redis ]
```

Data flow at a glance:

- **Roblox game servers** POST telemetry (snapshots, events) into CORE every ~10 seconds and poll for active config.
- **Lab compute nodes** (~9 machines) register with CORE, heartbeat every 15 seconds, and claim pipeline jobs (texture bakes, mesh optimization, light bakes, pathfinding pre-compute).
- **The React dashboard** subscribes over STOMP WebSocket for live state and issues mutations (create jobs, edit config) over REST.
- **CORE pushes to Roblox Open Cloud** (Messaging Service) so config activations reach live servers instantly, with the poll path as guaranteed fallback.

## Non-Goals (v1)

- No user accounts beyond static API keys.
- No multi-tenant support.
- No asset file storage inside CORE. Artifact files live on lab nodes / object storage; CORE tracks metadata and status only.

---

## Tech Stack

| Concern | Choice | Why |
|---|---|---|
| Language | Java 21 (LTS) | Virtual threads for cheap concurrency on polling/IO |
| Framework | Spring Boot 3.5.x | Ecosystem, WebSocket/STOMP, Actuator, Data Mongo |
| Build | Gradle 8.14 (Kotlin DSL, wrapper committed) | Fast, scriptable |
| Primary DB | MongoDB 7 | Telemetry and job payloads are schema-fluid JSON |
| Hot state / queues | Redis 7 | Job queue, node heartbeats, live config cache, pub/sub |
| Realtime to dashboard | WebSocket via STOMP (`spring-boot-starter-websocket`) | Topic-based broadcast maps cleanly to dashboard tabs |
| HTTP client (outbound) | Spring `RestClient` | Open Cloud calls |
| JSON | Jackson (bundled) | - |
| Local infra | Docker Compose (Mongo + Redis) | One-command dev env |

> Note: the original plan pinned Spring Boot 3.3.4, but that version has since dropped out of the supported range. The project is scaffolded on Boot 3.5.x, which satisfies the same requirements.

---

## Getting Started

### Prerequisites

- Java 21 (LTS)
- Docker (for local MongoDB + Redis)

### Run locally

```bash
# 1. Start infra
docker compose up -d

# 2. Boot the app
./gradlew bootRun

# 3. Verify
curl http://localhost:8080/actuator/health
# -> {"status":"UP"}

curl -H "X-Api-Key: dev-dash-key" http://localhost:8080/api/ping
# -> {"service":"shayveri-core","status":"ok"}

# Without a key, everything except /actuator/health is denied:
curl -i http://localhost:8080/api/ping
# -> 401/403
```

### Run tests

```bash
./gradlew test
```

Integration tests use Testcontainers (Mongo + Redis) rather than mocks, so Docker must be running.

---

## Project Layout

Package-by-module, not by layer. Each module owns its controllers, services, and documents.

```
dev.shayveri.core
├── config/            # Spring config: WebSocket, security, Mongo/Redis, virtual threads
├── common/            # Shared DTO envelopes, error handling, ApiKey principal, /api/ping
├── ingress/           # Module 1 - telemetry intake from Roblox
├── nodes/             # Module 2 - lab node registry + heartbeats
├── jobs/              # Module 3 - pipeline job queue + lifecycle
├── overrides/         # Module 4 - game config overrides + versioning
├── egress/            # Module 5 - Roblox Open Cloud client
├── realtime/          # Module 6 - WebSocket broadcast hub
└── observability/     # Module 7 - metrics, audit log
```

Repo root:

```
├── build.gradle.kts       # Boot 3.5.x, Java 21 toolchain, all starters
├── settings.gradle.kts
├── docker-compose.yml     # mongo:7 + redis:7-alpine
├── gradlew / gradle/      # committed wrapper (8.14.x)
└── src/main/resources/application.yml
```

---

## Security Model

Three static API-key roles, resolved from the `X-Api-Key` header by a `OncePerRequestFilter`. No sessions, CSRF off, fully stateless.

| Role | Who | Key env var | Dev default |
|---|---|---|---|
| `ROBLOX` | Roblox game servers | `SHAYVERI_KEY_ROBLOX` | `dev-roblox-key` |
| `NODE` | Lab node agents | `SHAYVERI_KEY_NODE` | `dev-node-key` |
| `DASH` | React dashboard | `SHAYVERI_KEY_DASH` | `dev-dash-key` |

`/actuator/health` is public; every other endpoint requires a valid key. Endpoint-level role restrictions (e.g. only `NODE` can claim jobs) are enforced per module as they are built.

---

## Configuration

`src/main/resources/application.yml`:

```yaml
spring:
  application.name: shayveri-core
  data:
    mongodb.uri: mongodb://localhost:27017/shayveri
    redis: { host: localhost, port: 6379 }
  threads.virtual.enabled: true

shayveri:
  security:
    api-keys:
      roblox: ${SHAYVERI_KEY_ROBLOX:dev-roblox-key}
      node:   ${SHAYVERI_KEY_NODE:dev-node-key}
      dash:   ${SHAYVERI_KEY_DASH:dev-dash-key}
  opencloud:
    api-key: ${ROBLOX_OPENCLOUD_KEY:}
    universe-id: ${ROBLOX_UNIVERSE_ID:}
    topic: shayveri-config

management:
  endpoints.web.exposure.include: health,metrics,prometheus
```

Secrets (Open Cloud API key, universe ID, production API keys) come from environment variables only and are never committed.

---

## Module Plan

Each module defines its responsibility, public surface (endpoints/events), the data it owns, and acceptance criteria. The public surfaces below are contracts: the React dashboard and node agents are built against them in parallel, so they must not drift.

### Module 1 - Ingress (Telemetry Intake)

**Package:** `ingress` · **Callers:** Roblox game servers (role `ROBLOX`)

Accept telemetry POSTs from live Roblox servers, validate, timestamp, persist, and fan out to the realtime hub. Must be cheap - this is the highest-frequency path (every server instance, ~every 10s).

**Endpoints**

- `POST /api/telemetry` - body: `{ placeId, jobId, playerCount, serverFps, round, customMetrics{} }`. Returns 202 immediately; persistence and broadcast are async (virtual thread executor).
- `POST /api/telemetry/events` - discrete events: player deaths (with position vectors for heatmaps), round transitions, perf spikes. Batched array accepted.

**Owns (Mongo collections)**

- `telemetry_snapshots` - TTL index, 7-day expiry (raw snapshots are disposable).
- `game_events` - no TTL; this feeds heatmaps and balance analysis.

**Emits:** realtime topic `/topic/telemetry/{placeId}` on every accepted snapshot.

**Acceptance:** Load-test 200 req/s sustained with p99 < 50ms; malformed payloads return 400 with field errors, never 500.

### Module 2 - Nodes (Lab Compute Registry)

**Package:** `nodes` · **Callers:** lab node agents (role `NODE`), dashboard (role `DASH`)

Track the ~9 lab machines: registration, capabilities (GPU, RAM, installed tooling e.g. Blender), heartbeat liveness, and current assignment.

**Endpoints**

- `POST /api/nodes/register` - node announces itself: `{ nodeId, hostname, capabilities{}, maxConcurrentJobs }`.
- `POST /api/nodes/{id}/heartbeat` - every 15s; body includes current load. Stored in Redis with 45s expiry - a node with no heartbeat key is DOWN, no scheduler needed.
- `GET /api/nodes` - dashboard view: all nodes + live status merged from Redis.

**Owns:** Mongo `nodes` (durable registry), Redis keys `node:hb:{id}` (liveness), `node:load:{id}`.

**Emits:** `/topic/nodes` on any status transition (UP to DOWN, idle to busy).

**Acceptance:** Kill a node agent; dashboard shows DOWN within 60s. Node re-registration is idempotent.

### Module 3 - Jobs (Pipeline Queue & Lifecycle)

**Package:** `jobs` · **Callers:** dashboard creates jobs, nodes claim/complete them

The heart of the optimization pipeline. Jobs are units of lab work: texture bake, mesh compression, light bake, PVS/pathfinding pre-compute. CORE never executes work - it schedules, tracks, and records.

**Job model**

```
Job { id, type: TEXTURE_BAKE|MESH_OPTIMIZE|LIGHT_BAKE|PATHFIND_PRECOMPUTE|CUSTOM,
      mapId, priority, payload{}, status: QUEUED|CLAIMED|RUNNING|DONE|FAILED,
      claimedBy, timings{}, resultRef (URI/path to artifact on node or object storage),
      resultMeta{} (e.g. tri counts before/after, texture sizes, bake duration) }
```

**Endpoints**

- `POST /api/jobs` (DASH) - enqueue; pushed to Redis list `jobs:queue:{type}` keyed by priority.
- `POST /api/jobs/claim` (NODE) - atomic claim (Redis `LMOVE`) filtered by node capability; long-poll up to 20s so node agents don't hammer.
- `POST /api/jobs/{id}/progress` (NODE) - percent + log lines, broadcast to dashboard.
- `POST /api/jobs/{id}/complete` / `/fail` (NODE) - result metadata or error; FAILED jobs requeue up to `maxRetries` with backoff.
- `GET /api/jobs?status=&mapId=` (DASH) - queue views.

**Owns:** Mongo `jobs` (full history - this is the research dataset: every bake's before/after metrics), Redis queues.

**Emits:** `/topic/jobs` (queue depth, transitions), `/topic/jobs/{id}` (progress stream).

**Acceptance:** Two nodes claiming concurrently never receive the same job. A node dying mid-job (heartbeat gone) triggers automatic requeue of its CLAIMED/RUNNING jobs.

### Module 4 - Overrides (Game Config Control Plane)

**Package:** `overrides` · **Callers:** dashboard writes, Roblox reads

Versioned game-variable overrides: weapon stats, zombie spawn rates, round pacing, graphics toggles. This is the "edit live from the dashboard" feature.

**Design:** One active **ConfigSet** per place (or global). Every save creates a new immutable version; the active pointer moves. Rollback = repoint. Full audit trail of who changed what.

**Endpoints**

- `GET /api/config/active?placeId=` (ROBLOX) - the long-poll/fallback path: Roblox servers fetch on interval. Served from Redis cache, ETag-supported so unchanged polls are ~free.
- `PUT /api/config` (DASH) - save new version; validates against a registered schema per config namespace (weapons, spawns, graphics) so a typo'd JSON key can't brick a live game.
- `POST /api/config/activate/{version}` (DASH) - repoint; triggers Module 5 push + Redis cache refresh.
- `GET /api/config/history` (DASH).

**Owns:** Mongo `config_versions`, Redis `config:active:{placeId}`.

**Emits:** `/topic/config` on activation.

**Acceptance:** Activating a version reaches a mock Roblox poller within one poll interval AND via Open Cloud push within ~2s. Rollback restores prior values exactly.

### Module 5 - Egress (Roblox Open Cloud Client)

**Package:** `egress` · **Callers:** internal only (Modules 3/4 trigger it)

All outbound calls to Roblox. v1 = **Open Cloud Messaging Service**: publish to a topic that live game servers subscribe to via `MessagingService`, so config activations hit running servers instantly instead of waiting for the next poll.

**Internal API (Spring service, not HTTP)**

- `publishConfigActivated(placeId, version)` - POST to `apis.roblox.com/messaging-service/v1/universes/{universeId}/topics/{topic}`.
- Retry with exponential backoff (3 attempts); on final failure, mark delivery DEGRADED and broadcast a dashboard warning - the polling path in Module 4 is the guaranteed fallback, so push failure is never fatal.

**Config:** API key + universe ID from env (never committed). Respect Open Cloud rate limits with a client-side token bucket.

**Acceptance:** With a bad API key, activation still succeeds (poll path) and dashboard shows a degraded-push warning. Message payloads stay under Roblox's 1KB messaging limit (send `{v: version}` pointer, not the config body - Roblox fetches the full config via Module 4 on receipt).

### Module 6 - Realtime (Dashboard WebSocket Hub)

**Package:** `realtime` · **Callers:** React dashboard (role `DASH`)

Single STOMP broker relay pushing all live state to the dashboard. Other modules call `RealtimePublisher.publish(topic, payload)` - they never touch WebSocket APIs directly.

**Topics**

| Topic | Content | Dashboard tab |
|---|---|---|
| `/topic/telemetry/{placeId}` | live snapshots | Tab 1 |
| `/topic/nodes` | node fleet status | Tab 2 |
| `/topic/jobs`, `/topic/jobs/{id}` | queue + progress | Tab 2 |
| `/topic/config` | activation events | Tab 3 |
| `/topic/alerts` | degraded push, node down, job failure storms | global |

**Design notes:** Handshake auth via `X-Api-Key` in the CONNECT frame (or query param) mapped to `DASH`. In-memory simple broker is fine for two users; leave the config seam to swap to an external Redis-backed broker later. Include a `GET /api/snapshot` REST endpoint returning current full state so the dashboard can hydrate on connect before deltas stream in.

**Acceptance:** Dashboard reconnect (kill wifi, restore) recovers full state within 3s via snapshot + resubscribe, no duplicate or missed transitions in the UI.

### Module 7 - Observability & Audit

**Package:** `observability`

Know what CORE is doing without SSH-ing anywhere.

- **Micrometer metrics** (via Actuator): telemetry ingest rate, queue depths (Redis gauge), Open Cloud call outcomes, WebSocket session count. Prometheus endpoint exposed; wire to Grafana later if wanted.
- **Audit log** (Mongo `audit`): every DASH mutation - who (api key label), what, before/after diff. With two people editing live game values, "who changed the spawn rate" needs a definitive answer.
- **Structured JSON logging** (logstash encoder) so lab-side log aggregation is trivial later.

**Acceptance:** `/actuator/prometheus` exposes custom `shayveri_*` metrics; every override activation appears in the audit collection.

---

## Build Order & Milestones

| Phase | Scope | Definition of done | Status |
|---|---|---|---|
| 0 | Scaffold, security filter, docker-compose, `/api/ping` | Boots green, key-gated ping | ✅ Scaffolded (boot verification pending local Docker) |
| 1 | Module 1 + Module 6 (minimal) | Fake Luau script (curl loop) sends telemetry, visible on a raw WebSocket client | ⬜ |
| 2 | Module 2 + Module 3 | A dummy Python node agent registers, claims a job, reports progress, completes | ⬜ |
| 3 | Module 4 + Module 5 | Config edit works via poll path; Open Cloud push verified against a real test universe | ⬜ |
| 4 | Module 7 + hardening | Metrics, audit, retry/requeue edge cases, load test on ingress | ⬜ |

---

## Development Rules

1. **One module per session/branch.** Keep changes scoped so each module lands as a coherent unit.
2. **Integration tests over mocks.** Test against real Mongo and Redis via Testcontainers.
3. **The public surface is a contract.** Endpoints, payloads, and topic names must match this document exactly - the React dashboard and node agents are built against it in parallel.
4. **Secrets never committed.** Open Cloud keys and production API keys come from environment variables.
5. **Storage behind interfaces.** Each module defines a small store interface for its persistence needs (e.g. `JobStore`, `HeartbeatStore`, `ConfigStore`); the Mongo/Redis implementations are adapters behind it. Business logic never touches a Mongo/Redis API directly. This keeps each storage slot independently swappable (see the asdb item under v2 ideas).

---

## Deferred / v2 Ideas (do not build yet)

- Heatmap aggregation service (server-side binning of death positions).
- Object storage (MinIO) for artifact files instead of node-local paths.
- OAuth-backed dashboard login replacing static DASH key.
- Job DAGs (bake, then compress, then bundle as a chained pipeline instead of independent jobs).
- Per-map A/B config experiments with automatic telemetry comparison.
- **asdb integration:** swap one or more storage slots to asdb (custom Rust database) behind the existing store interfaces, starting with a low-risk slot (e.g. node heartbeats), once its server protocol and primitives (key expiry, atomic list ops) are confirmed.
