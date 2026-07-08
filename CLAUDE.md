# Esports Tracker – Project Specification

## Overview
A personal esports tracking platform where users select which games and teams they follow, and get a unified dashboard of upcoming tournaments, match scores, schedules, and standings – across multiple esports titles.

**Game scope:** League of Legends and Dota 2 to start. PandaScore (originally planned as the single data source for all titles) restructured its free tier to schedules-only (no scores/standings) at some point after this spec was first written, and CS2/Valorant have no viable official free data source. LoL (Riot's public esports feed) and Dota 2 (Valve's Steam Web API) both have real official APIs with free access to schedules, results, and standings, so the project is scoped to those two for now. More titles can be added later if a legitimate free source turns up for them.

**Purpose:** Portfolio project to demonstrate Spring Boot, REST API design, relational database modeling, external API integration, and testing. Built by a CS student (graduating Sept 2026) to strengthen backend skills for job applications.

**No AI/LLM features.** This is a pure backend project.

## Current Progress (update this section as phases complete)

**Phase 1 (Foundation) — done.** Game entity/CRUD, User registration + JWT auth, Spring Security config, tests.

**Phase 2 (Core Data) — done.** League/Team/Tournament/Match entities + migrations, `RiotEsportsClient` (LoL only — Dota 2/Steam client is not built, only config placeholders exist), `RiotSyncService` + scheduler polling live Riot data, Tournament/Match REST endpoints (paginated), 64 tests. Verified end-to-end against real Riot data (42 leagues, 488+ tournaments, growing match history).

**Phase 3 (Personalization) — done.** `User.followedGames`/`followedTeams` (many-to-many, full-replace semantics) + migrations V10/V11, `PUT /api/users/me/games`/`teams` (validates slugs/ids exist before replacing), `GET /api/matches/upcoming` and `GET /api/feed` (both query by followed games/teams with OR semantics, using a placeholder-UUID trick to keep `IN (...)` clauses non-empty), all auth-required. 79 tests (up from 64), plus manual end-to-end verification against real synced Riot data (register → login → follow → upcoming/feed → 401/404/400 edge cases all confirmed).

**Phase 4 (Polish) — partially started.** Standings (step 17) done: `Standing` entity/migration (V12), `RiotEsportsClient.getStandings`, `RiotSyncService.syncStandings`, `GET /api/tournaments/{id}/standings`. 83 tests (up from 79), verified end-to-end against real Riot data (e.g. LEC Split 2 2026's finished regular-season table matches Riot's response exactly). Pagination (step 15) was pulled forward into Phase 2's Tournament/Match endpoints rather than deferred, and RFC 7807 error handling (step 18) has been in place since Phase 1. Swagger/OpenAPI dependency is present in `pom.xml` but not yet verified/customized. Dota 2/Steam is intentionally out of scope for now (LoL-only working version first).

**Still not done from Phase 3's scope:** `RiotSyncService.syncMatches()` is still scoped to "in-season" leagues (a tournament active or starting within 14 days) rather than user-follow-driven, as originally planned once the follow model existed. Revisit this now that follows are built.

**Known gaps worth knowing about:** no `@DataJpaTest` repository tests exist yet (Testing Strategy below mentions them as a target). `GET /api/games/{slug}/teams` and `/api/games/{slug}/tournaments` were deliberately skipped as redundant with the filtered list endpoints (`/api/tournaments?game=`, etc.) — revisit only if a more RESTful nested-resource style is wanted later.

## Working With Claude
The user is building this project to learn, not just to have it built. In every session:
- Always apply industry best practices for the relevant layer (Spring Boot, JPA, REST API design, security, testing) rather than the shortest path to something that runs.
- Explain the *why* behind architectural and implementation decisions as you make them — what problem the pattern solves, what tradeoffs exist, and why it fits here specifically (not generic textbook explanations).
- When there are multiple valid approaches, briefly mention the alternative(s) and why the chosen one wins in this context.
- Favor teaching moments over silently doing the "correct" thing — assume the user wants to be able to explain these decisions themselves later (e.g. in interviews).
- **Design decisions require explicit confirmation before implementation.** This includes things like: what attributes/fields an entity should have, table/column design, endpoint shapes, which library/pattern to use, etc. Propose the design with your reasoning and options, then wait for the user's go-ahead before writing the code. The user wants to stay the one steering the project's design, not just review it after the fact. Purely mechanical follow-through (e.g. running a build, fixing a compile error) doesn't need this.

## Tech Stack
- **Framework:** Spring Boot 3.x (Java 21)
- **Build Tool:** Maven
- **Database:** PostgreSQL (with Spring Data JPA / Hibernate)
- **API Documentation:** SpringDoc OpenAPI (Swagger UI)
- **Testing:** JUnit 5 + Mockito + Spring Boot Test
- **External Data Sources:** Riot's public LoL Esports API (League of Legends) + Valve's Steam Web API (Dota 2) — see "External API Integration" below
- **Migration:** Flyway for database versioning
- **Optional Frontend:** None initially. The project is API-first – all functionality exposed via REST endpoints. A frontend (React or Thymeleaf) can be added later.

## Architecture

### Layered Structure
```
src/main/java/dev/mundorf/esportstracker/
├── controller/        # REST controllers
├── service/           # Business logic
│   └── sync/          # RiotSyncService + RiotSyncScheduler (reconciliation + cron)
├── repository/        # Spring Data JPA repositories
├── model/
│   ├── entity/        # JPA entities
│   └── dto/           # Request/Response DTOs
├── client/
│   └── riot/           # RiotEsportsClient + RiotClientConfig
│       └── dto/         # Raw Riot API response shapes (not our internal DTOs)
├── security/           # JwtService, JwtAuthenticationFilter, CustomUserDetailsService
├── config/             # SecurityConfig, JpaAuditingConfig, SchedulingConfig
├── exception/          # Custom exceptions + global handler
└── mapper/             # Entity <-> DTO mapping
```
A Steam/Dota 2 client would live at `client/steam/` following the same pattern once built.

### Core Entities

**User**
- id, username, email, passwordHash, createdAt
- Follows games (many-to-many, `user_followed_games`, full-replace semantics via `replaceFollowedGames`)
- Follows teams (many-to-many, `user_followed_teams`, full-replace semantics via `replaceFollowedTeams`)

**Game**
- id, name, slug (e.g. "league-of-legends", "dota-2"), iconUrl
- Source: seeded manually (just LoL + Dota 2 for now)

**Team**
- id, name, slug (unique per game), logoUrl, game (many-to-one), externalId (unique per game)
- Source: synced from Riot (LoL) / Valve (Dota 2)

**League** (a named recurring competition series — LEC, LCK, and also Worlds/MSI/TI, since Riot's actual API structures international events as leagues too, just with `region="INTERNATIONAL"` instead of a country/region name)
- id, name, slug (unique per game), region (nullable, e.g. "EMEA", "Korea", "INTERNATIONAL"), game (many-to-one), externalId (unique per game)
- Source: synced from Riot (LoL); sparse/rarely populated for Dota 2, which doesn't have Riot-style recurring franchised leagues

**Tournament**
- id, name, slug (unique per game), league (many-to-one, **required** — every tournament has a parent league, matching Riot's real data model where even Worlds/MSI have one), game (many-to-one), startDate, endDate (LocalDate), tier (enum: INTERNATIONAL/PRIMARY/SECONDARY, derived at sync time from `league.region` — INTERNATIONAL region → INTERNATIONAL tier, known majors like LEC/LCK/LPL/LCS → PRIMARY, else SECONDARY — but still stored directly on Tournament for simple indexed filtering rather than joining through League every query), status (upcoming/running/finished), prizePool (nullable, may not be populated depending on what the APIs actually expose), externalId (unique per game)
- Source: synced from Riot (LoL) / Valve (Dota 2)

**Match**
- id, tournament (many-to-one), game (many-to-one), teamA (many-to-one), teamB (many-to-one), scheduledAt (Instant), status (upcoming/running/finished, shared enum with Tournament.status), scoreA, scoreB (nullable until finished), streamUrl, externalId (unique per game)
- Source: synced from Riot (LoL) / Valve (Dota 2)

**Standing** (a team's position within one ranked table of a tournament)
- id, tournament (many-to-one), team (many-to-one), groupName (String — Riot's section name, e.g. "Regular Season"; disambiguates tournaments with more than one ranked table), rank (Riot's ordinal; ties share a rank), wins, losses
- No `draws` field (dropped from the original tentative proposal — LoL is best-of-series, never a draw)
- No `externalId` — unlike League/Team/Tournament/Match, a Riot ranking is a derived aggregate with no stable id of its own; upserts key off `UNIQUE(tournament_id, team_id, group_name)` instead
- Bracket/playoff stages have no win-loss table (Riot returns an empty ranking list for them) and are never represented here — see Match for bracket data instead
- Source: synced from Riot (LoL) via `getStandings`, piggybacking on the same 15-min cadence and "in-season" tournament scope as match sync

### REST Endpoints

**Auth & Users**
```
POST   /api/auth/register          – Register new user                                    [built]
POST   /api/auth/login             – Login (returns JWT)                                   [built]
GET    /api/users/me               – Get current user profile (auth required)              [built]
PUT    /api/users/me/games         – Update followed games                                 [built]
PUT    /api/users/me/teams         – Update followed teams                                 [built]
```

**Games** — the two sub-resource routes were deliberately skipped as redundant with the filtered list endpoints below
```
GET    /api/games                  – List all supported games                              [built]
GET    /api/games/{slug}           – Get game details                                      [built]
```

**Tournaments** — list/detail/matches paginated (`page`/`size`/`sort` query params, `PagedResponse` envelope); standings is not, since a group table is always small
```
GET    /api/tournaments            – List (filter: game, status, tier)                     [built]
GET    /api/tournaments/{id}       – Tournament details                                    [built]
GET    /api/tournaments/{id}/matches – Matches in a tournament, paginated                   [built]
GET    /api/tournaments/{id}/standings – Standings for a tournament, by group then rank     [built]
```

**Matches** — list/detail/today paginated where applicable
```
GET    /api/matches                – List (filter: game, team, status, from/to date range)  [built]
GET    /api/matches/today          – Today's matches across all games                       [built]
GET    /api/matches/{id}           – Match details                                          [built]
GET    /api/matches/upcoming       – Upcoming matches for followed games/teams (auth req.)   [built]
```

**Feed (personalized)**
```
GET    /api/feed                   – Combined feed: upcoming matches + running tournaments for followed games/teams (auth required)   [built]
```

### External API Integration

Two separate clients instead of one unified provider, since LoL and Dota 2 come from different companies with different API shapes. Each implements the same conceptual contract (fetch leagues/tournaments, fetch schedule, fetch standings) so the sync scheduler and mapper layer can treat them uniformly even though the wire formats differ.

HTTP client: Spring's `RestClient` (Framework 6.1+), not `RestTemplate` (maintenance mode) or `WebClient` (reactive, needs the WebFlux dependency — unnecessary for a blocking scheduled sync job). Configured with explicit connect/read timeouts via `ClientHttpRequestFactorySettings`.

**LoL Esports API (Riot) — implemented, `client/riot/`**
- **Base URL:** `https://esports-api.lolesports.com/persisted/gw/`
- **Auth:** `x-api-key` header. Not a secret Riot ever tried to protect — the same key value is hardcoded client-side by lolesports.com itself and documented across dozens of open-source projects — but it's still stored as `${RIOT_ESPORTS_API_KEY}`, not hardcoded, so it can be swapped if Riot ever rotates it.
- **Rate limit:** undocumented/unofficial API, no published limit — poll conservatively regardless.
- Endpoints actually used: `getLeagues`, `getTournamentsForLeague`, `getSchedule` (params: `leagueId`), `getEventDetails` (resolves the authoritative tournament ID + stable team IDs that `getSchedule` alone doesn't expose)
- Not yet used: `getCompletedEvents`

**Dota 2 Steam Web API (Valve) — NOT implemented.** Config placeholders exist (`steam.api-key`, `steam.base-url`) but no client, no DTOs, no sync. Blocked on the user registering a free Steam API key. When built, follow the same pattern as `client/riot/`.
- **Base URL:** `https://api.steampowered.com/IDOTA2Match_570/`
- **Auth:** `key` query parameter — a free personal Steam API key from steamcommunity.com/dev/apikey
- **Rate limit:** no officially published limit for this endpoint group; poll conservatively
- Planned endpoints: `GetLiveLeagueGames`, `GetMatchHistory`, `GetLeagueListing`, `GetMatchDetails`

**Sync Strategy** (implemented for Riot/LoL in `RiotSyncService` + `RiotSyncScheduler`; Dota 2 not built):
- `@Scheduled` cron jobs, matching cadence to data volatility
- `syncLeaguesAndTournaments` (`sync.tournaments-cron`, every 6h): **every** league Riot returns (~40+), not filtered — gives a full catalog for a future "choose leagues to follow" settings UI. Tier derived from `league.region`/slug at sync time (see Tournament entity above).
- `syncMatches` (`sync.matches-cron`, every 15m): scoped to "in-season" leagues only — those with a `Tournament` currently `UPCOMING` or `RUNNING` within a 14-day horizon — not all leagues every cycle. **Planned change, still not done:** drive this by user-follow data instead, now that Phase 3's follow model exists.
- Reconciliation is **upsert-by-`externalId`**, never delete-and-recreate: existing rows updated in place via each entity's `update(...)` method, new rows inserted. Idempotent and self-healing (a corrected score on Riot's side is picked up next poll).
- Never expose Riot/Valve response payloads directly – always map to internal entities via provider-specific DTOs in `client/<provider>/dto/`

### Database Schema Notes
- All entities use auto-generated UUIDs as primary keys
- `externalId` (String) on League, Team, Tournament, Match for sync deduplication against Riot/Valve — a single string column rather than separate typed columns per provider, since Riot serializes its IDs as strings anyway (they're 64-bit values large enough to lose precision as JS numbers) and Valve's numeric IDs store fine as their string form too. Each entity already has exactly one provider (via its `game`), so no separate provider column is needed
- `slug` and `externalId` are unique **per game** (`UNIQUE (game_id, ...)`), not globally unique — Riot's and Valve's ID/name spaces are independent, so the same raw value could coincidentally appear in both without meaning anything
- Use `@CreatedDate` and `@LastModifiedDate` on all entities
- Flyway migrations in `src/main/resources/db/migration/`
- Index: match.scheduledAt, match.status, tournament.status, team.game_id, tournament.league_id

### Testing Strategy
- **Unit tests:** Service layer with mocked repositories (Mockito, `ArgumentCaptor` to verify the exact entity that would be persisted). Pure/stateless helper logic (e.g. `RiotSyncService`'s tier/status derivation, name prettification) is `static` and package-private, not `private` — lets tests call it directly via `@ParameterizedTest` instead of only exercising it indirectly through full orchestration.
- **Controller tests:** `@WebMvcTest`, real mapper beans imported via `@Import(...)` (verifies actual JSON shape, not just "a mock was called"), services mocked via `@MockBean`. **Important gotcha:** `JwtAuthenticationFilter` is a `@Component` implementing `Filter`, so `@WebMvcTest` auto-detects and constructs it regardless of `@AutoConfigureMockMvc(addFilters = false)` — that flag only skips *applying* filters, not *building* the bean — which cascades into needing a live `UserRepository`. Exclude it explicitly: `@WebMvcTest(controllers = X.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))`. The one exception is any controller using `@AuthenticationPrincipal` (e.g. `UserController`) — there, keep the filter chain enabled and use `@WithMockUser` instead, since disabling filters would let unauthenticated requests reach the controller and NPE on a null principal rather than being rejected realistically.
- **Live integration tests:** for external API clients and sync logic, name the test class `*LiveIT` (e.g. `RiotEsportsClientLiveIT`) — Maven Surefire's default include pattern (`**/*Test.java`, `**/*Tests.java`) does NOT match `*IT.java`, so these are automatically skipped by a plain `mvn test` and only run explicitly via `mvn test -Dtest=ClassName`. A third-party API/DB being unavailable should never fail the regular build.
- **Repository tests:** `@DataJpaTest` with embedded H2 or Testcontainers (PostgreSQL) — **not built yet**, no repository-layer tests exist.
- Target: every service method has at least one happy-path and one edge-case test
- Use meaningful test names: `shouldReturnUpcomingMatchesForFollowedTeams()`

### Security
- JWT-based authentication (Spring Security + jjwt)
- Public endpoints: game listing, tournament listing, match listing, registration, login
- Protected endpoints: feed, user profile, follow/unfollow
- Passwords hashed with BCrypt

## Build Order (incremental)

### Phase 1 – Foundation ✅ done
1. Initialize Spring Boot project with dependencies (Web, JPA, PostgreSQL, Security, Validation, OpenAPI)
2. Set up Flyway + first migration (User, Game tables) — split into separate per-entity migrations (V1 Game, V2 User) rather than one combined migration
3. Implement Game entity, repository, service, controller
4. Implement User registration + JWT auth
5. Write tests for Game service and Auth flow

### Phase 2 – Core Data ✅ done (LoL/Riot only — Dota 2/Steam not implemented)
6. Add Team, Tournament, Match entities + migrations — plus `League`, added after the fact once Riot's real data model turned out to need it (see Core Entities above)
7. Implement Riot LoL Esports client (Valve Dota 2 client not yet built) using Spring's `RestClient`, not `RestTemplate`/`WebClient` — see External API Integration above for why
8. Build sync scheduler for leagues/tournaments/matches (not just "games and teams" as originally scoped — Team sync ended up folded into match sync, since Riot only exposes team data embedded in match/event payloads, not via a standalone teams endpoint)
9. Implement Tournament and Match endpoints — paginated from the start, pulling Phase 4 step 15 forward
10. Write tests for sync logic and endpoints — 64 tests, see Testing Strategy above

### Phase 3 – Personalization ✅ done
11. ✅ Implement user follows (games + teams). `RiotSyncService.syncMatches()`'s "in-season leagues" scoping to prioritize actually-followed leagues instead/additionally is still **not done** (noted in Current Progress above)
12. ✅ Build personalized feed endpoint
13. ✅ Build "upcoming matches for my teams" endpoint
14. ✅ Write tests for personalization logic — 79 tests total (up from 64), plus manual end-to-end verification against real synced Riot data

### Phase 4 – Polish
15. ✅ done — filtering/pagination already built into the Tournament/Match endpoints (Phase 2 step 9), pulled forward rather than deferred
16. Add Swagger/OpenAPI documentation — dependency present in `pom.xml`, not yet configured/verified
17. ✅ Add standings sync and endpoint — 83 tests total (up from 79), plus manual + live-IT verification against real Riot data (see Current Progress above)
18. ✅ done — `GlobalExceptionHandler` + RFC 7807 `ProblemDetail` responses have been in place since Phase 1, refined incrementally (404/409/401 handlers added as each feature needed them)
19. Write integration tests — the `*LiveIT` tests (see Testing Strategy) partially cover this; broader `@SpringBootTest` integration coverage not yet done
20. Clean up README with setup instructions, API examples, architecture diagram — basic run instructions exist in README.md, API examples/architecture diagram not yet added

## Configuration

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/esportstracker
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway handles schema
    show-sql: false
    open-in-view: false  # forces explicit fetching in the service layer; see @EntityGraph usage in repositories
  flyway:
    enabled: true
  mvc:
    problemdetails:
      enabled: true  # RFC 7807 bodies for built-in errors (e.g. @Valid failures)
  jackson:
    deserialization:
      fail-on-unknown-properties: false  # external API responses can add fields over time; our DTOs only map what we use

riot:
  esports-api-key: ${RIOT_ESPORTS_API_KEY}
  base-url: https://esports-api.lolesports.com/persisted/gw

steam:
  api-key: ${STEAM_API_KEY}
  base-url: https://api.steampowered.com/IDOTA2Match_570

jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 86400000  # 24h

sync:
  matches-cron: "0 */15 * * * *"      # every 15 min
  tournaments-cron: "0 0 */6 * * *"   # every 6 hours
```

## External API Keys
- **Riot LoL Esports API:** no registration needed — uses the long-standing public key `0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z`, the same one lolesports.com's own frontend uses. Still kept in `RIOT_ESPORTS_API_KEY` rather than hardcoded, in case Riot ever rotates it.
- **Valve Steam Web API:** free personal key at https://steamcommunity.com/dev/apikey (requires a Steam account).

## Local Dev Environment
- Postgres runs in a Docker container named `esports-tracker-db` (Postgres 16, db `esportstracker`, user/password `esports`/`esports`, port 5432). Start it with `docker start esports-tracker-db` if stopped (check first: `docker ps -a --filter name=esports-tracker-db`) — it may stop if Docker Desktop restarts or the machine sleeps.
- Run the app: `./mvnw spring-boot:run` with `DB_USERNAME`, `DB_PASSWORD`, `RIOT_ESPORTS_API_KEY`, `STEAM_API_KEY` (placeholder fine, unused until Dota 2 is built), `JWT_SECRET` set as env vars — see README.md.
- Static test console at `http://localhost:8080/` (register/login/me + public games list) — see `src/main/resources/static/index.html`.

## Git Strategy
- Main branch: `main` (always deployable)
- Feature branches: `feature/entity-name` or `feature/endpoint-name`
- Commit messages: conventional commits (`feat:`, `fix:`, `test:`, `docs:`)
- .gitignore: IDE files, build output, .env, application-local.yml
