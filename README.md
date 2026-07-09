# SHAYVERI CORE

**SHAYVERI CORE** (Compute Orchestration & Rendering Engine) is the Spring Boot backend of the SHAYVERI Roblox pipeline: it ingests live game telemetry, orchestrates optimization jobs across lab compute nodes, and manages versioned live-game config pushed via Roblox Open Cloud. Runs standalone with just Docker (Mongo + Redis).

CORE is the single source of truth for the whole system. Roblox game servers and lab compute nodes are clients that push data in and poll/receive config out; a React dashboard is the read/write control surface over WebSocket + REST; CORE alone talks to the Roblox Open Cloud API.

## Quick Start

Prerequisites: Java 21, Docker.

```bash
docker compose up -d        # start local MongoDB + Redis
./gradlew bootRun           # boot the service

# verify
curl http://localhost:8080/actuator/health
# -> {"status":"UP"}
curl -H "X-Api-Key: dev-dash-key" http://localhost:8080/api/ping
# -> {"service":"shayveri-core","status":"ok"}
```

Every endpoint except `/actuator/health` requires an `X-Api-Key` header. Dev keys: `dev-roblox-key` (game servers), `dev-node-key` (lab nodes), `dev-dash-key` (dashboard). Production keys come from environment variables and are never committed.

## Layout

```
src/main/java/dev/shayveri/core/   the service, package-by-module:
  common/ config/                  shared pieces + Spring configuration
  ingress/ nodes/ jobs/ overrides/ egress/ realtime/ observability/
                                   one folder per module (see the plan)
Idea_Generation/plan.txt           THE PLAN: full module specs, milestones,
                                   acceptance criteria
```

## Status

| Phase | Scope | State |
|---|---|---|
| 0 | Scaffold, security filter, dev infra, `/api/ping` | Done (boot verification pending Docker install) |
| 1 | Telemetry ingress + live WebSocket feed | Next |
| 2 | Node registry + job queue | - |
| 3 | Config control plane + Open Cloud push | - |
| 4 | Observability + hardening | - |

## Documentation

- **The plan** (module specs, contracts, milestones): `Idea_Generation/plan.txt`
- Module 1 construction blueprint: `Idea_Generation/docs/module1_blueprint.txt`
- Pre-Phase-1 summary (structure, decisions, gitignore explained): `Idea_Generation/PrePhase1_Summary.pdf`
