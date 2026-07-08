# Esports Tracker

A personal esports-tracking backend: pick which games and teams you follow, get a unified feed
of upcoming matches, running tournaments, and standings — synced from official esports APIs.

**Portfolio project** demonstrating Spring Boot 3, layered REST API design, JPA/Postgres data
modeling, external-API integration with scheduled reconciliation, JWT security, and multi-level
testing (unit → slice → repository → full-stack integration → live API contract). LoL only for
now (Riot's public esports API); the architecture is provider-agnostic so a second title can be
plugged in without touching the core.

See [CLAUDE.md](CLAUDE.md) for the full project specification, the decisions behind it, and the
build order.

## Tech stack

Java 21 · Spring Boot 3.3 · Spring Security (JWT via jjwt) · Spring Data JPA / Hibernate 6 ·
PostgreSQL 16 · Flyway · Spring's `RestClient` · JUnit 5 · Mockito · AssertJ · Maven · Docker

## Architecture

```mermaid
flowchart LR
    Client([HTTP client]) -->|JSON, JWT bearer| API[Spring Boot REST layer]
    API -->|JPA| DB[(PostgreSQL<br/>Flyway migrations V1–V12)]
    Scheduler[RiotSyncScheduler<br/>@Scheduled cron] -->|upsert-by-externalId| DB
    Scheduler -->|RestClient| Riot[Riot LoL Esports API]

    subgraph API layers
        direction TB
        Controllers[controller/] --> Services[service/]
        Services --> Repositories[repository/]
        Services --> Mappers[mapper/]
    end
```

Every synced entity (`League`, `Team`, `Tournament`, `Match`, `Standing`) reconciles via
upsert-by-`externalId` rather than delete-and-recreate, so the sync job is idempotent and
self-healing (a corrected score on Riot's side is picked up on the next poll). Match sync runs
every 15 minutes but is scoped to "in-season" leagues (a tournament active or within 14 days) —
per-user views filter this shared fresh dataset at read time, so the poll cost stays bounded by
the number of active tournaments instead of scaling with users.

## Prerequisites

- **Java 21** (JDK)
- **Docker** (for PostgreSQL — see below)
- No local Maven install needed — the project uses the Maven Wrapper (`mvnw`).

## Getting started

### 1. Start PostgreSQL

```bash
docker run -d --name esports-tracker-db \
  -e POSTGRES_DB=esportstracker \
  -e POSTGRES_USER=esports \
  -e POSTGRES_PASSWORD=esports \
  -p 5432:5432 \
  postgres:16
```

Already created? `docker start esports-tracker-db`.

### 2. Set environment variables

```bash
export DB_USERNAME=esports
export DB_PASSWORD=esports
export RIOT_ESPORTS_API_KEY=0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z
export STEAM_API_KEY=unused-for-now
export JWT_SECRET=a-long-random-secret-string-at-least-48-bytes-please-change-me
```

Windows PowerShell: `$env:DB_USERNAME = "esports"` (etc.).

The Riot key above is the same one lolesports.com's own frontend uses — no registration needed.
See [CLAUDE.md](CLAUDE.md#external-api-keys) for why it's kept in an env var anyway.
`STEAM_API_KEY` is a placeholder; Dota 2 support is intentionally out of scope for the current
LoL-only working version.

### 3. Run

```bash
./mvnw spring-boot:run
```

Flyway applies all 12 migrations on first boot. The sync scheduler starts polling Riot on its
cron cadence (matches every 15 min, leagues/tournaments every 6h — nothing fires on boot, so
restarts don't hammer Riot).

A minimal test console is served at <http://localhost:8080/> (register/login/me + public games
list).

## API tour

Interactive docs are also live at <http://localhost:8080/swagger-ui.html> once the app is
running (raw OpenAPI 3 JSON at `/v3/api-docs`). The JWT-protected endpoints are marked with a
lock icon; click **Authorize** and paste the token from `/api/auth/login` to try them from the
UI. A typical end-to-end flow, in curl:

All endpoints return `application/json`, with `Problem+JSON` (RFC 7807) shapes for errors.

### Public: browse tournaments and matches

```bash
curl http://localhost:8080/api/tournaments?status=RUNNING&size=5
curl http://localhost:8080/api/matches?game=league-of-legends&status=UPCOMING
curl http://localhost:8080/api/matches/today
```

Standings for a specific tournament (grouped by section, ordered by rank):

```bash
curl http://localhost:8080/api/tournaments/{id}/standings
```

```json
[
  {"groupName": "Regular Season", "rank": 1, "wins": 8, "losses": 1,
   "team": {"name": "Team Vitality", "slug": "team-vitality", "logoUrl": "..."}},
  {"groupName": "Regular Season", "rank": 2, "wins": 7, "losses": 2,
   "team": {"name": "Karmine Corp", "slug": "karmine-corp", "logoUrl": "..."}}
]
```

### Register and log in

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}'
# → {"token":"eyJhbGciOi..."}

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password123"}' \
  | jq -r .token)
```

### Follow, then get a personalized feed

```bash
curl -X PUT http://localhost:8080/api/users/me/games \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"slugs":["league-of-legends"]}'

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/feed
```

```json
{
  "upcomingMatches": [
    {"scheduledAt": "2026-07-09T08:00:00Z", "status": "UPCOMING",
     "teamA": {"name": "G2 Esports"}, "teamB": {"name": "T1"},
     "tournamentName": "MSI 2026", "gameSlug": "league-of-legends"}
  ],
  "runningTournaments": [
    {"name": "MSI 2026", "tier": "INTERNATIONAL", "status": "RUNNING",
     "startDate": "2026-06-27", "endDate": "2026-07-12",
     "league": {"name": "MSI", "region": "INTERNATIONAL"}}
  ]
}
```

### Full endpoint list

| Method | Path | Auth | Notes |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | — | Returns JWT |
| `POST` | `/api/auth/login` | — | Returns JWT |
| `GET` | `/api/games` | — | List supported games |
| `GET` | `/api/games/{slug}` | — | |
| `GET` | `/api/tournaments` | — | Filter: `game`, `status`, `tier`. Paginated |
| `GET` | `/api/tournaments/{id}` | — | |
| `GET` | `/api/tournaments/{id}/matches` | — | Paginated |
| `GET` | `/api/tournaments/{id}/standings` | — | Grouped by section, ordered by rank |
| `GET` | `/api/matches` | — | Filter: `game`, `team`, `status`, `from`, `to`. Paginated |
| `GET` | `/api/matches/today` | — | Today (UTC) |
| `GET` | `/api/matches/{id}` | — | |
| `GET` | `/api/matches/upcoming` | JWT | Upcoming matches for followed games/teams |
| `GET` | `/api/users/me` | JWT | Includes followed games/teams |
| `PUT` | `/api/users/me/games` | JWT | Full replace |
| `PUT` | `/api/users/me/teams` | JWT | Full replace |
| `GET` | `/api/feed` | JWT | Combined feed for follows |

## Testing

```bash
./mvnw test
```

88 tests across four layers:

- **Service unit tests** — mocked repositories, `ArgumentCaptor` on the saved entity
- **Controller slice** — `@WebMvcTest` with real mapper beans imported, services mocked → verifies actual JSON shape
- **Repository** — `@DataJpaTest` against in-memory H2 in Postgres-compat mode; Flyway runs the real V1–V12 migrations so entities are validated against the production schema
- **Full-stack integration** — one `@SpringBootTest` end-to-end run through register → login → follow → `/api/feed`

Live tests that hit the real Riot API or Postgres are named `*LiveIT.java` and deliberately
excluded from `mvn test` (Surefire's default include pattern is `*Test.java`/`*Tests.java`).
Run them explicitly when you want to exercise external behavior:

```bash
./mvnw test -Dtest=RiotEsportsClientLiveIT      # hits the real Riot API
./mvnw test -Dtest=RiotSyncServiceLiveIT        # + writes to the configured Postgres
```

A third-party API being flaky should never fail the regular build.

## Notable design decisions

Depth is in [CLAUDE.md](CLAUDE.md); the highlights:

- **`RestClient`, not `RestTemplate` or `WebClient`** — Spring 6.1's blocking fluent client
  suits a scheduled sync job without dragging in WebFlux.
- **Sync is in-season-scoped, not follow-driven** — poll cost stays bounded by active
  tournaments instead of scaling with user count. Follows filter at read time.
- **`externalId` as a single `String` column** — Riot's IDs are 64-bit numeric-strings; each
  entity has exactly one provider (via its `game`), so no provider-typing column is needed.
- **`slug`/`externalId` unique per game, not globally** — Riot's and any future provider's ID
  spaces are independent.
- **Standings have no `externalId`** — unlike other synced entities, Riot rankings are derived
  aggregates with no stable id; upserts key off `(tournament, team, groupName)` instead.
- **Deliberately not built:** repository tests for trivial derived queries, follow-driven sync
  polling, Dota 2/Steam client (intentionally out of scope for the LoL-only baseline).

## Project structure

```
src/main/java/dev/mundorf/esportstracker/
├── controller/        REST controllers
├── service/
│   └── sync/          RiotSyncService + RiotSyncScheduler
├── repository/        Spring Data JPA repositories
├── model/
│   ├── entity/        JPA entities
│   └── dto/           Request/Response DTOs
├── client/riot/       RiotEsportsClient + provider DTOs
├── security/          JwtService, JwtAuthenticationFilter, CustomUserDetailsService
├── config/            SecurityConfig, JpaAuditingConfig, SchedulingConfig
├── exception/         Custom exceptions + GlobalExceptionHandler (RFC 7807)
└── mapper/            Entity ↔ DTO mapping
```
