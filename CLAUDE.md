# Esports Tracker – Project Specification

## Overview
A personal esports tracking platform where users select which games and teams they follow, and get a unified dashboard of upcoming tournaments, match scores, schedules, and standings – across multiple esports titles.

**Game scope:** League of Legends, Dota 2, and Apex Legends (added 2026-07-10). PandaScore (originally planned as the single data source for all titles) restructured its free tier to schedules-only (no scores/standings) at some point after this spec was first written, and CS2/Valorant have no viable official free data source. LoL (Riot's public esports feed) and Dota 2 (Valve's Steam Web API) both have real official APIs with free access to schedules, results, and standings. Apex was researched 2026-07-10: **EA/Respawn publish no esports API at all** (confirmed via EA's own forums and by inspecting algs.ea.com directly - its schedule is server-rendered by a Next.js RSC app with no client-visible data endpoint, so even scraping would be brittle); apexlegendsstatus.com's community "Ingram" tournament API turned out to contain only community scrim lobbies, not real ALGS results (verified with a real token - the "ALGS"-named entries there are practice lobbies). The source that actually worked is the **Cito API** (citoapi.com), a third-party aggregator of official ALGS results with a genuine free tier - accepted with eyes open as the same risk class as PandaScore was (commercial third party, free tier could change). More titles can be added later if a legitimate free source turns up for them.

**Purpose:** Portfolio project to demonstrate Spring Boot, REST API design, relational database modeling, external API integration, and testing. Built by a CS student (graduating Sept 2026) to strengthen backend skills for job applications.

**No AI/LLM features.** This is a pure backend project — the React frontend (added in Phase 5) is a thin consumer of the REST API, not a feature in its own right.

## Current Progress (update this section as phases complete)

**Phase 1 (Foundation) — done.** Game entity/CRUD, User registration + JWT auth, Spring Security config, tests.

**Phase 2 (Core Data) — done.** League/Team/Tournament/Match entities + migrations, `RiotEsportsClient` (LoL only — Dota 2/Steam client is not built, only config placeholders exist), `RiotSyncService` + scheduler polling live Riot data, Tournament/Match REST endpoints (paginated), 64 tests. Verified end-to-end against real Riot data (42 leagues, 488+ tournaments, growing match history).

**Phase 3 (Personalization) — done.** `User.followedGames`/`followedTeams` (many-to-many, full-replace semantics) + migrations V10/V11, `PUT /api/users/me/games`/`teams` (validates slugs/ids exist before replacing), `GET /api/matches/upcoming` and `GET /api/feed` (both query by followed games/teams with OR semantics, using a placeholder-UUID trick to keep `IN (...)` clauses non-empty), all auth-required. 79 tests (up from 64), plus manual end-to-end verification against real synced Riot data (register → login → follow → upcoming/feed → 401/404/400 edge cases all confirmed).

**Phase 4 (Polish) — done for the LoL-only baseline.** Standings (step 17): `Standing` entity/migration (V12), `RiotEsportsClient.getStandings`, `RiotSyncService.syncStandings`, `GET /api/tournaments/{id}/standings`. Broader integration tests (step 19): `@DataJpaTest` coverage for the three custom queries with real Flyway-driven schema, plus one full-stack `@SpringBootTest` (`FeedFlowIntegrationTest`) exercising register → login → follow → feed end-to-end. 88 tests (up from 79). Pagination (step 15) was pulled forward into Phase 2's Tournament/Match endpoints. RFC 7807 error handling (step 18) has been in place since Phase 1. README polish (step 20): full portfolio-oriented rewrite with architecture diagram, curl walkthrough, endpoint reference, design-decisions section. Swagger/OpenAPI (step 16): `OpenApiConfig` + JWT bearer security scheme, JWT-protected endpoints annotated with `@SecurityRequirement(name = "bearer-jwt")` so Swagger UI shows a lock icon and the "Authorize" button just on those; Swagger UI live at `/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs`. Dota 2/Steam is intentionally out of scope for now (LoL-only working version first).

**Phase 5 (React frontend) — done for the baseline described here.** `frontend/`: Vite + React + TypeScript, Tailwind CSS v4, TanStack Query, React Router. Auth (JWT in localStorage, `AuthContext`, `ProtectedRoute`), public match/tournament browsing with pagination, tournament detail with standings, a personalized feed page, and a Following/settings page. Dev server proxies `/api` to `:8080` (`vite.config.ts`) so no CORS changes were needed on the backend. Not a graded phase in the original spec — added because a portfolio project benefits from a live demo, not just curl output.

**Match details (per-game stats) — done.** `GET /api/matches/{id}/details` returns champions/items/KDA/objectives per game of a series, clickable from any match card. Deliberately **not synced** — proxied from Riot's undocumented `feed.lolesports.com/livestats` feed at request time (no API key needed) behind a 60-second Caffeine cache (`spring-boot-starter-cache` + `caffeine`, `CacheConfig`), because this data is display-only, frame-level huge, and rarely viewed — syncing it would bloat the DB for little benefit and still be too stale for live games. The feed only serves frames around a requested `startingTime` and never signals when a game ended, so finding a finished game's final state requires a bounded forward-walk until a frame reports `gameState: "finished"` (`MatchDetailsService.walkToFinalFrame`) — an undocumented API quirk discovered by direct probing, not written down anywhere official. Champion/item icons are fetched client-side straight from Riot's Data Dragon CDN.

**Decision (2026-07-08):** the earlier plan to make `RiotSyncService.syncMatches()` follow-driven has been **rejected**. Sync stays in-season-scoped; user follows are a *read-time filter* over the fully-synced dataset (which is how `/api/feed` and `/api/matches/upcoming` already work), not a driver of the poll cadence. Reasons: poll cost stays bounded by the number of active tournaments instead of scaling with users, and every user reads from the same fresh cache instead of each follow triggering its own staleness. The "still not done" note this replaces was left over from an earlier design that made less sense as the follow model landed.

**Decision (2026-07-09): follow model changed from game-level to league-level.** Following an entire game (all ~40 LoL leagues at once) flooded the feed with minor/national leagues alongside the ones a user actually cares about. `User.followedLeagues` (many-to-many, `user_followed_leagues`, migration V13) was added; `/api/feed` and `/api/matches/upcoming` now match on **followed leagues ∪ followed teams** only. `followedGames` still exists and is still settable via `PUT /api/users/me/games`, but it's a **UI grouping only** — it decides which game's leagues show in settings and no longer widens any feed query. `GET /api/leagues?game=` was added (leagues previously had no standalone endpoint, only nested inside `TournamentResponse`) so the frontend can offer a league picker; `LeagueResponse.tier` is derived at read time via `TournamentTier.forLeague(region, slug)` (shared with the sync-time tournament tier derivation, so the two can never disagree) and drives a collapsible International/Primary/National grouping in the settings UI. Tournament follows were considered and explicitly rejected — a league follow already covers every seasonal split, so a third follow type would mostly just go stale.

**Team browser (2026-07-10) — done.** Closes the "no standalone team endpoint" gap noted below. `GET /api/teams` (paginated, `?game=`/`?search=` filters, case-insensitive name search) and `GET /api/teams/{id}` (team detail: organization, roster, standings, live/upcoming/recent matches). New `Organization` entity (migration V14) that `Team` optionally belongs to — exists so a future second game's teams could link to the same org as their LoL counterpart, though today it's 1:1 since Dota 2 isn't synced. New `Player` entity (migration V14) synced from Riot's `getTeams` endpoint (`RiotEsportsClient.getTeams`, `RiotSyncService.syncTeamsAndRosters`, same 6h cadence as `syncLeaguesAndTournaments`) — full-replace reconciliation per team (a departed player actually disappears, not just goes stale). Roster has no starter/substitute flag from Riot (confirmed by direct API probing), so `PlayerResponse.active` is a **heuristic**: a player counts as active if they appeared in one of the team's last 5 finished matches (cross-referenced against the already-proxied match-details data — see `TeamService.findActiveSummonerNames`/`isActive`), not an authoritative signal. `Team.league` (migration V15, resolved from Riot's `homeLeague` field by name match) lets a team page trigger a **backgrounded, throttled** re-sync of that team's league on visit (`TeamSyncTrigger`, `@Async` + `AsyncConfig`, throttled via the same Caffeine cache mechanism as match details) — the page always renders instantly from whatever's in the DB and never blocks on Riot; a delayed `useTeamDetail` refetch (~4s) picks up anything the background sync found, with a small "Updating matches…" indicator while that refetch is in flight. Frontend: `TeamSearch` (nav dropdown, debounced), `TeamsPage` (full search results, big centered search bar, syncs with URL changes on re-search), `TeamDetailPage` (upcoming → roster grouped active/bench → results with full season names + place emoji → recent matches), `FollowTeamHeaderButton` (Follow/Followed/Unfollow-on-hover).

**Live-match visibility (2026-07-10) — done.** A match transitioning from `UPCOMING` to `RUNNING` used to have nowhere to display: the Feed only queried `UPCOMING` matches and `RUNNING` tournaments (not matches), and team pages only queried `UPCOMING`/`FINISHED`. Added `MatchRepository.findRunningForFollowed`/`MatchService.findLiveForUser`, wired into `FeedResponse.liveMatches` and `TeamDetailResponse.liveMatches`, both rendered as a "Live now" section. `StatusBadge` now takes a `kind: 'match' | 'tournament'` prop: a Match's `RUNNING` status is Riot's own `inProgress` state (a game is genuinely being played, shown as "LIVE"), but a Tournament/League's `RUNNING` status just means today falls within its date range (`RiotSyncService.deriveStatus`) — shown as "ongoing" instead, since it says nothing about whether a game is on stream right now.

**Watch links (2026-07-10, expanded 2026-07-10) — done.** Match pages show Twitch/YouTube/`watch.lolesports.com` links for `UPCOMING`/`RUNNING` matches (`WatchLinks`, `leagueChannels.ts`). Confirmed by direct probing that Riot's `getEventDetails` only populates a `streams` array once a broadcast is nearly live (empty even 21h out), and `getLeagues` has no channel data at all — so this is a small curated map, not sourced from the API. Twitch only shows for leagues with a genuinely verified handle — an earlier `twitch.tv/lolesports` guess for unlisted leagues was wrong (not a real channel) and was removed; unlisted leagues just don't show a Twitch button. Coverage was expanded from the original LEC/LCK/LCS/international-only set via direct web research per league (not guessed): added `lpl`, `cblol-brazil`, `ljl-japan`, `lla`, `lco`, `primeleague`, `vcs`, and `lcp` (the 2024+ tier-1 Pacific league — its channel `lolpacificen` was *not* mapped to `pcs`, since PCS is now a tier-2 feeder into LCP and no PCS-specific channel was found). `turkiye-sampiyonluk-ligi`/`lcl` were researched but deliberately left out — only a general "Riot Games regional" channel turned up for TCL (not TCL-specific), and LCL has been inactive since 2022. YouTube (`youtube.com/@lolesports`) and the official site are shown unconditionally as a universal fallback.

**Team browser/live-match test coverage (2026-07-10) — done.** Closed the gap below: `TeamServiceTest`, `TeamControllerTest`, `TeamSyncTriggerTest`, `PlayerMapperTest`, `TeamMapperTest`, `MatchService.findLiveForUser` tests (mirroring `findUpcomingForUser`'s existing coverage), and `RiotSyncService.syncTeamsAndRosters`/`syncTeamAndRoster`/`syncLeagueOnDemand`/roster-full-replace tests (following the file's existing "pure function tests" / "orchestration tests" split). 153 tests total (up from 101).

**Follow-update race hardening (2026-07-10) — done.** Two tabs (or a second toggle firing before an earlier one's response updated the UI) computing full-replace follow PUTs from a stale cached profile used to silently overwrite each other — last write wins, no error. Fixed with API-level optimistic concurrency: `User.version` (`@Version`, migration V16), `UserResponse.version` round-tripped by the frontend and resubmitted on every follow PUT (`FollowGamesRequest`/`FollowTeamsRequest`/`FollowLeaguesRequest.version`), `UserService` rejects a mismatched version as `StaleUpdateException` → 409 before applying the change. **Non-obvious gotcha found via direct DB inspection, not assumption:** Hibernate's automatic `@Version` increment only fires when the owning entity's *own row* is dirtied — a pure `@ManyToMany` join-table change (add/remove a follow) never touches the `users` row itself, so the version silently never bumped even though the follow change itself worked correctly (confirmed by checking `version`/`updated_at` directly in Postgres after a real follow update: both stayed unchanged). Fixed by explicitly calling `entityManager.lock(user, LockModeType.OPTIMISTIC_FORCE_INCREMENT)` after every replace (`UserService.bumpVersion`) — the JPA-blessed way to force a version bump when only an association changed. Frontend (`useFollowGames`/`Teams`/`Leagues`) reads the current version from the cached `['me']` query and resubmits it; a 409 invalidates `['me']` to force a refetch of the real current state rather than leaving the UI showing a stale optimistic view.

**Apex Legends / ALGS support (2026-07-10) — done, end to end.** Third game, first non-Riot data source, and the first Battle Royale format. Key decisions and findings:
- **Data source: Cito API** (`client/cito/`, `cito.api-key`/`CITO_API_KEY` env var, key registered at citoapi.com/signup). See Game scope above for why (no official EA API exists; the community Ingram API only has scrim lobbies — both verified directly, not assumed). **Free tier is 500 calls/month**, which shaped the whole sync design: `CitoClient.getAlgsEvents()` makes exactly ONE call per cycle (the events list embeds schedules AND full results together), `CitoSyncService` runs only on the 6h `tournaments-cron` (~120 calls/month), and there is deliberately no on-demand/per-page-visit sync like LoL's `TeamSyncTrigger` — an Apex team page never triggers one (guarded in `TeamService.findById`, which now only fires the Riot trigger for LoL teams).
- **BR format gets its own entities, not force-fit into Match/Standing** (migration V17): `ApexMatchDay` (the Apex analog of Match — ~20 teams at once, no teamA/teamB), `ApexTeamResult` (one team's rank + total points per day, upsert key `UNIQUE(match_day_id, team_id)` like Standing), `ApexGameResult` (per-game placement/kills/points, diff-based full replace like roster sync). Forcing BR results into `Match` (nullable teamB, repurposed scores) was considered and rejected — it would corrupt those columns' meaning for every head-to-head game too.
- **ALGS regions ARE leagues**: Cito has no league catalog, so `CitoSyncService` maps its five region strings to League rows (`algs-americas`/`algs-emea`/`algs-apac-north`/`algs-apac-south` → PRIMARY tier, added to `TournamentTier.PRIMARY_LEAGUE_SLUGS`; `algs-global` → region INTERNATIONAL → INTERNATIONAL tier, matching how Worlds/MSI work). This plugs ALGS straight into the existing league-follow model — the settings picker, feed queries, and tier grouping all work unchanged. Tournaments are one per `yearSlug`+`eventSlug` (e.g. "ALGS Year 6 Split 1 Pro League EMEA"), with date ranges **grown incrementally** from match day dates since Cito has no tournament-level dates.
- **Teams are upserted by slugified name** (Cito exposes names only — no ids, no logos) with the event's region stamped as `Team.league`. They show up in the existing team search and follow flows automatically (same `teams` table). **Feed team-follow twist:** a pending BR match day has no team list (teams only appear once results land), so `ApexMatchDayService.findUpcomingForUser` resolves a followed Apex team to its home league — following an Apex team implies following its region's schedule. This widening is Apex-only and deliberately NOT applied to LoL team follows (covered by a dedicated test).
- **Endpoints:** `GET /api/apex/matchdays` (paginated, `?league=`/`?status=`), `GET /api/apex/matchdays/{id}` (results table + per-game breakdowns, fetched via one `findByTeamResultIdIn` query, not per-team). `FeedResponse.apexMatchDays` (upcoming, for followed leagues/teams); `TeamDetailResponse.apexResults` (a team's last 5 day placements). Cito's `pending`/`completed` map to UPCOMING/FINISHED — there is no RUNNING state for Apex.
- **Frontend:** `/apex` (list + region/status filters), `/apex/:id` (ranked results, rows expand to per-game tables — rows are clickable divs, not buttons, because they contain the follow-star button and nested buttons are invalid HTML), "ALGS" nav item, "Upcoming ALGS match days" feed section (only rendered when non-empty), Apex-specific team page layout (recent day placements instead of the LoL roster/matches sections, which would read as permanently broken-empty).
- **Non-obvious fix found during browser verification:** `UserRepository.findWithFollowsByUsername`'s entity graph had to grow `followedTeams.game` and `followedTeams.league` — the feed's team→league resolution runs after the session closes (`open-in-view: false`), and hit a `LazyInitializationException` that only manifested against the real running app, not in mocked tests.
- 179 backend tests total (up from 154): `CitoSyncServiceTest` (same pure-function/orchestration split as `RiotSyncServiceTest`), `ApexMatchDayServiceTest`, `ApexMatchDayControllerTest`, plus guard tests in `TeamServiceTest`. Verified end-to-end against the real Cito API: 50 real ALGS events synced (Year 6 Pro League + EWC Playoffs), 120 teams, 720 team results, 4320 game results; browse/detail/follow/feed/search all exercised in the browser.

**React error boundary (2026-07-10) — done.** Closed the gap below: `ErrorBoundary` (class component — no hook equivalent exists) wraps `<Outlet/>` inside `Layout`, keyed by `location.pathname` so navigating to a different route always clears a previous error. Scoped around the route content rather than the whole app so the header/nav (and a way to navigate elsewhere) stay usable when one page's render throws, instead of blanking everything.

**Other fixes this session:** tournament list now sorts by tier severity (INTERNATIONAL → PRIMARY → SECONDARY) via explicit `CASE` rather than relying on `EventStatus`'s alphabetical-happens-to-match-severity coincidence (`TournamentRepository.search`); tournament detail's match list sorts descending (next match + most recent results first) instead of ascending from the tournament's opening day; `RiotSyncService.prettifyTournamentName` now renders full season names (Winter/Spring/Summer) instead of "Split N" for both textual and numbered slugs (`SPLIT_NUMBER_TO_SEASON`); match cards show a self-adjusting live countdown (`MatchCard.useCountdown` — ticks every second once under an hour out, every minute otherwise, to avoid needless re-renders for far-future matches).

**Known gaps worth knowing about:**
- `GET /api/games/{slug}/teams` and `/api/games/{slug}/tournaments` were deliberately skipped as redundant with the filtered list endpoints (`/api/tournaments?game=`, etc.) — revisit only if a more RESTful nested-resource style is wanted later.
- `Organization` exists but is untested cross-game (Dota 2 still isn't synced), so its "same org across games" purpose is unproven — it's currently just a 1:1 wrapper around `Team`. Not actionable until a second game is synced.
- Watch links are still a curated, manually-maintained map (`frontend/src/api/leagueChannels.ts`) — every league without a confidently-verified handle (see above) shows no Twitch button, just YouTube + the official site. Expanding further means repeating the same direct-research process per league, not guessing from a naming pattern.

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
- **Frontend (Phase 5):** React 19 + TypeScript, built with Vite, styled with Tailwind CSS v4, server state via TanStack Query, routing via React Router. The project stayed API-first through Phase 4 (all functionality exposed via REST endpoints, verified with curl/Swagger) before the frontend was added as a thin consumer — see `frontend/` and the Frontend section below.

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
│   └── riot/           # RiotEsportsClient + RiotLiveStatsClient + RiotClientConfig
│       └── dto/         # Raw Riot API response shapes (not our internal DTOs)
├── security/           # JwtService, JwtAuthenticationFilter, CustomUserDetailsService
├── config/             # SecurityConfig, JpaAuditingConfig, SchedulingConfig, CacheConfig, AsyncConfig
├── exception/          # Custom exceptions + global handler
└── mapper/             # Entity <-> DTO mapping

frontend/               # Vite + React + TypeScript SPA (Phase 5) — see its own section below
```
A Steam/Dota 2 client would live at `client/steam/` following the same pattern once built. `RiotEsportsClient` talks to `esports-api.lolesports.com` (catalog/schedule data, synced); `RiotLiveStatsClient` is a separate client for `feed.lolesports.com` (per-game live stats, fetched on demand — see Match Details below), since it's a different host with different auth (none) and a different usage pattern (proxy, not sync).

### Core Entities

**User**
- id, username, email, passwordHash, createdAt, version (`@Version`, migration V16 — optimistic concurrency for follow updates, see Current Progress: follow-update race hardening)
- Follows games (many-to-many, `user_followed_games`, full-replace semantics via `replaceFollowedGames`) — **UI grouping only, does not drive the feed** (see Current Progress: follow model decision)
- Follows leagues (many-to-many, `user_followed_leagues`, full-replace semantics via `replaceFollowedLeagues`) — drives the feed together with followed teams
- Follows teams (many-to-many, `user_followed_teams`, full-replace semantics via `replaceFollowedTeams`) — drives the feed together with followed leagues
- Follow-update PUTs (`updateFollowedGames`/`Teams`/`Leagues`) take a client-submitted `expectedVersion`; a mismatch throws `StaleUpdateException` → 409, before the replace is applied. Because a `@ManyToMany` join-table-only change never dirties the `users` row on its own (confirmed by direct DB inspection — Hibernate's automatic version bump doesn't fire for that case), each update explicitly forces the increment via `entityManager.lock(user, LockModeType.OPTIMISTIC_FORCE_INCREMENT)` (`UserService.bumpVersion`) rather than relying on Hibernate's automatic dirty-checking.

**Game**
- id, name, slug (e.g. "league-of-legends", "dota-2"), iconUrl
- Source: seeded manually (just LoL + Dota 2 for now)

**Team**
- id, name, slug (unique per game), logoUrl, game (many-to-one), externalId (unique per game), organization (many-to-one, nullable), league (many-to-one, nullable — a team's "home league", used to trigger on-demand sync from its detail page)
- `organization`/`league` are set only by `RiotSyncService.syncTeamAndRoster` (from Riot's `getTeams`), separately from the narrower `upsertTeam` driven by match/standings sync — a team appearing mid-cycle in a live match never fails on a missing FK while waiting for the roster sync to run
- Source: synced from Riot (LoL) / Valve (Dota 2)

**Organization** (an esports org, meant to group `Team` rows that represent the same brand across different games)
- id, name, slug (**globally** unique, unlike Team's per-game slug), logoUrl
- Currently 1:1 with Team, since only LoL is synced — the grouping exists so linking logic is already in place whenever a second game's sync is built, not so it backfills anything today
- Source: synced from Riot (LoL) via `getTeams`, upserted by slug

**Player** (a roster entry for a Team)
- id, team (many-to-one), summonerName, firstName, lastName, imageUrl, role (enum: TOP/JUNGLE/MID/BOTTOM/SUPPORT/NONE — declaration order doubles as display order), externalId (unique per team)
- No starter/substitute flag — Riot's `getTeams` doesn't expose one (confirmed by direct probing); `PlayerResponse.active` is computed at read time as a heuristic instead (see Current Progress: Team browser)
- Source: synced from Riot (LoL) via `getTeams`, **full-replace per team** (a departed player is deleted, not left stale) on the same 6h cadence as `syncLeaguesAndTournaments`

**League** (a named recurring competition series — LEC, LCK, and also Worlds/MSI/TI, since Riot's actual API structures international events as leagues too, just with `region="INTERNATIONAL"` instead of a country/region name)
- id, name, slug (unique per game), region (nullable, e.g. "EMEA", "Korea", "INTERNATIONAL"), game (many-to-one), externalId (unique per game)
- No `tier` column — `LeagueResponse.tier` is derived at read time via `TournamentTier.forLeague(region, slug)`, the same logic Tournament sync uses to stamp its own (persisted) tier, so the two can never drift apart
- Source: synced from Riot (LoL); sparse/rarely populated for Dota 2, which doesn't have Riot-style recurring franchised leagues

**Tournament**
- id, name, slug (unique per game), league (many-to-one, **required** — every tournament has a parent league, matching Riot's real data model where even Worlds/MSI have one), game (many-to-one), startDate, endDate (LocalDate), tier (enum: INTERNATIONAL/PRIMARY/SECONDARY, derived at sync time from `league.region` — INTERNATIONAL region → INTERNATIONAL tier, known majors like LEC/LCK/LPL/LCS → PRIMARY, else SECONDARY — but still stored directly on Tournament for simple indexed filtering rather than joining through League every query), status (upcoming/running/finished), prizePool (nullable, may not be populated depending on what the APIs actually expose), externalId (unique per game)
- Source: synced from Riot (LoL) / Valve (Dota 2)

**Match**
- id, tournament (many-to-one), game (many-to-one), teamA (many-to-one), teamB (many-to-one), scheduledAt (Instant), status (upcoming/running/finished, shared enum with Tournament.status), scoreA, scoreB (nullable until finished), streamUrl, externalId (unique per game)
- Source: synced from Riot (LoL) / Valve (Dota 2)

**ApexMatchDay** (a Battle Royale "match day", e.g. ALGS "Group A vs B" on a given date — the Apex analog of Match, with ~20 teams instead of teamA/teamB; see Current Progress: Apex Legends)
- id, tournament (many-to-one, required), game (many-to-one, required), name, startsAt (Instant), status (UPCOMING/FINISHED only — Cito has no live state), externalId (Cito's ULID, unique per game)
- Source: synced from the Cito API (aggregated official ALGS data) by `CitoSyncService`, 6h cadence only

**ApexTeamResult** (one team's cumulative result across a match day's games: rank + total points)
- id, matchDay (many-to-one), team (many-to-one), rank (`rank_position` column, same reserved-word workaround as Standing), totalPoints
- No externalId — derived aggregate like Standing; upserts key off `UNIQUE(match_day_id, team_id)`

**ApexGameResult** (one team's result in a single game of a match day)
- id, teamResult (many-to-one), gameNumber, placement, kills, points; `UNIQUE(team_result_id, game_number)`
- Full-replace via diffing on sync (a game Cito stops reporting actually disappears), same pattern as roster sync

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
PUT    /api/users/me/games         – Update followed games (UI grouping only, not feed input) [built]
PUT    /api/users/me/leagues       – Update followed leagues (feed input)                  [built]
PUT    /api/users/me/teams         – Update followed teams (feed input)                    [built]
```

**Games** — the two sub-resource routes were deliberately skipped as redundant with the filtered list endpoints below
```
GET    /api/games                  – List all supported games                              [built]
GET    /api/games/{slug}           – Get game details                                      [built]
```

**Leagues** — flat, unpaginated (~42 rows for LoL, same reasoning as standings)
```
GET    /api/leagues                – List all leagues, optionally filtered by ?game=       [built]
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
GET    /api/matches/{id}/details   – Per-game stats (champions/items/KDA/objectives), proxied on demand from Riot, not synced [built]
GET    /api/matches/upcoming       – Upcoming matches for followed leagues/teams (auth req.) [built]
```

**Teams** — list paginated (`?game=`/`?search=` filters); detail is not
```
GET    /api/teams                  – Search/browse teams (filter: game, search by name)     [built]
GET    /api/teams/{id}             – Team detail: org, roster, standings, live/upcoming/recent matches [built]
```

**Apex Legends (ALGS)** — list paginated (`?league=`/`?status=` filters); detail is not (a results table is always one day's worth)
```
GET    /api/apex/matchdays         – Browse ALGS match days (filter: league slug, status)    [built]
GET    /api/apex/matchdays/{id}    – Match day detail: ranked results + per-game breakdowns  [built]
```

**Feed (personalized)**
```
GET    /api/feed                   – Combined feed: live + upcoming matches, running tournaments, upcoming ALGS match days — all for followed leagues/teams (auth required)   [built]
```

### External API Integration

Separate clients per provider instead of one unified abstraction, since each game's data comes from a different company with a different API shape. Each implements the same conceptual contract (fetch leagues/tournaments, fetch schedule, fetch standings) so the sync scheduler and mapper layer can treat them uniformly even though the wire formats differ.

HTTP client: Spring's `RestClient` (Framework 6.1+), not `RestTemplate` (maintenance mode) or `WebClient` (reactive, needs the WebFlux dependency — unnecessary for a blocking scheduled sync job). Configured with explicit connect/read timeouts via `ClientHttpRequestFactorySettings`.

**LoL Esports API (Riot) — implemented, `client/riot/`**
- **Base URL:** `https://esports-api.lolesports.com/persisted/gw/`
- **Auth:** `x-api-key` header. Not a secret Riot ever tried to protect — the same key value is hardcoded client-side by lolesports.com itself and documented across dozens of open-source projects — but it's still stored as `${RIOT_ESPORTS_API_KEY}`, not hardcoded, so it can be swapped if Riot ever rotates it.
- **Rate limit:** undocumented/unofficial API, no published limit — poll conservatively regardless.
- Endpoints actually used: `getLeagues`, `getTournamentsForLeague`, `getSchedule` (params: `leagueId`), `getEventDetails` (resolves the authoritative tournament ID + stable team IDs that `getSchedule` alone doesn't expose)
- Not yet used: `getCompletedEvents`

**Apex Legends / Cito API — implemented, `client/cito/`** (see Current Progress: Apex Legends for the full source-selection story)
- **Base URL:** `https://api.citoapi.com/api/v1/`
- **Auth:** `x-api-key` header, `${CITO_API_KEY}` — free self-serve key from citoapi.com/signup
- **Rate limit / budget:** free tier is **500 calls/month** — the binding constraint. Sync makes exactly one call per 6h cron tick (`getAlgsEvents` returns 50 match days WITH embedded results); never add per-event or on-demand calls without re-doing this math.
- Endpoint used: `GET /apex/algs/events` (schedule + statsData together). NOT used from sync: `/events/{id}/scores` (redundant with the embedded statsData), legend/global-stats endpoints (display-only depth, not worth the calls).

**Dota 2 Steam Web API (Valve) — NOT implemented.** Config placeholders exist (`steam.api-key`, `steam.base-url`) but no client, no DTOs, no sync. Blocked on the user registering a free Steam API key. When built, follow the same pattern as `client/riot/`.
- **Base URL:** `https://api.steampowered.com/IDOTA2Match_570/`
- **Auth:** `key` query parameter — a free personal Steam API key from steamcommunity.com/dev/apikey
- **Rate limit:** no officially published limit for this endpoint group; poll conservatively
- Planned endpoints: `GetLiveLeagueGames`, `GetMatchHistory`, `GetLeagueListing`, `GetMatchDetails`

**Sync Strategy** (implemented for Riot/LoL in `RiotSyncService` + `RiotSyncScheduler`; Dota 2 not built):
- `@Scheduled` cron jobs, matching cadence to data volatility
- `syncLeaguesAndTournaments` (`sync.tournaments-cron`, every 6h): **every** league Riot returns (~40+), not filtered — gives a full catalog for the "choose leagues to follow" settings UI (built in Phase 5, `GET /api/leagues`). Tier derived from `league.region`/slug at sync time (see Tournament entity above); `TournamentTier.forLeague(...)` holds the actual logic so tournament sync and the read-time `LeagueResponse.tier` share one implementation.
- `syncTeamsAndRosters` (`sync.tournaments-cron`, same 6h cadence — rosters change on transfer-window timescales, not live-match timescales): the full team catalog from Riot's `getTeams` (one call, all games/leagues) — organization, logo, `homeLeague` (resolved to our `League` by name match), and roster (full-replace per team). Independent of the narrower `upsertTeam` below.
- `syncMatches` (`sync.matches-cron`, every 15m): scoped to "in-season" leagues only — those with a `Tournament` currently `UPCOMING` or `RUNNING` within a 14-day horizon — not all leagues every cycle. Deliberately **not** driven by user follows (earlier plan, rejected — see Current Progress above): poll cost stays bounded by active tournaments instead of scaling with users, and per-user views filter this same fresh dataset at read time. Also upserts `Team` rows it encounters (narrower than `syncTeamsAndRosters` — no organization/league/roster, just enough to satisfy the match's FK).
- `syncLeagueOnDemand(League)`: same logic as `syncMatches`, but for one league, triggered by a team page visit (`TeamSyncTrigger`, fire-and-forget `@Async`) rather than the cron — throttled via a Caffeine cache (`leagueSyncThrottle`) so repeat visits within 60s are a no-op. Never blocks the request that triggers it. **LoL teams only** — `TeamService.findById` guards on game slug, since the trigger hits Riot's schedule endpoint and Apex refreshes solely on its cron (Cito budget).
- `CitoSyncService.syncAlgsEvents` (`sync.tournaments-cron`, same 6h cadence, own scheduler `CitoSyncScheduler`): one Cito call per tick upserts ALGS leagues (curated region→league map), tournaments (date ranges grown incrementally from event dates), match days, teams (by slugified name), and results (`ApexGameResult` rows diff-replaced per team result). Runs ONLY on the slow cron — see the call-budget note above.
- Reconciliation is **upsert-by-`externalId`**, never delete-and-recreate: existing rows updated in place via each entity's `update(...)` method, new rows inserted. Idempotent and self-healing (a corrected score on Riot's side is picked up next poll). `Player` rows are the one exception — roster sync is **full-replace** per team (delete-then-recreate semantics via diffing, not upsert-only), since a departed player should actually disappear.
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
- **Repository tests:** `@DataJpaTest` against in-memory H2 in PostgreSQL-compat mode (`src/test/resources/application-test.yml`) — Flyway still runs the real V1–V12 migrations so entities are validated against the actual production schema. Kept to the *non-trivial custom queries* only (`MatchRepository.findUpcomingForFollowed`, `TournamentRepository.findRunningForFollowed`, `StandingRepository.findByTournamentIdOrderByGroupNameAscRankAsc`) — derived queries and Riot-facing code are already covered elsewhere. Testcontainers would only pay off if we introduced Postgres-specific SQL (JSONB, custom types).
- **`@DataJpaTest` gotchas that bit once and are worth knowing:** (1) `@DataJpaTest` doesn't scan `@Configuration` classes, so `@EnableJpaAuditing` from `JpaAuditingConfig` isn't active by default and `@CreatedDate`/`@LastModifiedDate` stay null — every save then fails on `NOT NULL created_at`. Fix: `@Import(JpaAuditingConfig.class)` on the test. (2) `@AutoConfigureTestDatabase(replace = NONE)` is required so Spring Boot doesn't swap our carefully-configured H2 URL for its own random test DB and skip Flyway.
- **Full-stack integration test:** one, `FeedFlowIntegrationTest` (`@SpringBootTest` + `MockMvc` + H2), exercises the whole register → login → follow → `/api/feed` chain through the real security/JPA/mapper/controller stack, including proving that following just a game (no league/team) leaves the feed empty. Proves integration wiring works — the sort of bug (missing `@EntityGraph`, security misroute, JSON shape mismatch) that layer-mocked tests can't catch. `RiotEsportsClient` is `@MockBean`'d so a stray cron tick can't hit the real Riot API. **Must be `@Transactional`:** it shares H2's in-memory DB with the `@DataJpaTest` classes, and without a rolling-back transaction its inserts leak into the shared DB and break the next test class that seeds the same tables.
- Target: every service method has at least one happy-path and one edge-case test — met, including the team browser/live-match code added 2026-07-10 (see Current Progress: team browser/live-match test coverage)
- Use meaningful test names: `shouldReturnUpcomingMatchesForFollowedTeams()`
- 179 tests total as of 2026-07-10 (up from 153 earlier the same day; Apex/Cito coverage added)
- **No frontend test suite yet.** The React app is verified manually/via browser preview per feature (see Phase 5), not with an automated test runner — acceptable for a portfolio project where the backend is the graded surface, but worth naming explicitly as a gap rather than leaving it implicit.

### Security
- JWT-based authentication (Spring Security + jjwt)
- Public endpoints: game listing, league listing, tournament listing, match listing, team listing, registration, login
- Protected endpoints: feed, user profile, follow/unfollow
- Passwords hashed with BCrypt

### Frontend (`frontend/`)
- **Structure:** `src/api/` (typed `fetch` wrapper + TanStack Query hooks — DTOs hand-mirrored from the backend's `model/dto` records, kept in sync manually since there's no shared-types/OpenAPI-codegen step), `src/auth/` (`AuthContext`, `ProtectedRoute`), `src/components/` (cards, badges, layout), `src/pages/` (one per route).
- **Auth:** JWT stored in `localStorage`, attached as a `Bearer` header by the API client; `AuthContext` exposes `isAuthenticated`/`user`/`login`/`logout`; `ProtectedRoute` redirects unauthenticated visitors to `/login` and remembers where they were headed.
- **Data fetching:** all server state goes through TanStack Query hooks in `src/api/queries.ts` — no component calls `fetch` directly. Follow mutations (`useFollowGames`/`useFollowLeagues`/`useFollowTeams`) invalidate `['me']`, `['feed']`, and `['matches','upcoming']` on success, matching the backend's full-replace PUT semantics. Each mutation reads the current `version` off the cached `['me']` query and resubmits it (optimistic concurrency — see Current Progress: follow-update race hardening); a 409 response invalidates `['me']` so the UI refetches the real current state instead of showing a stale optimistic one.
- **Error handling:** `ErrorBoundary` (`src/components/ErrorBoundary.tsx`) wraps `<Outlet/>` in `Layout`, keyed by route path — an unhandled render error shows a fallback with a "Try again" button in the content area while the header/nav stay usable, rather than blanking the whole app.
- **Routing:** React Router; logged-in users land on `/` (the feed), logged-out visitors land on `/matches` (public browsing).
- **Match details UI:** per-game tabs (one per Bo3/Bo5 game) showing champion icons, KDA, CS, gold, item builds, and team objectives, sourced from `GET /api/matches/{id}/details`. Champion/item icons are fetched directly from Riot's Data Dragon CDN in the browser — static assets are never proxied through our API.
- **Follow model in the UI:** the Following/settings page shows a game checklist (UI grouping — decides which leagues are shown, not what's in the feed) and, per followed game, a collapsible International/Primary/National league picker (order matches `TournamentTier`). Teams are followed inline via a star toggle on match cards and standings rows (`FollowTeamButton`), or via a full text button on a team's own page (`FollowTeamHeaderButton` — "Follow"/"Followed", swaps to "Unfollow" on hover while followed).
- **Team browser:** `TeamSearch` in the nav (debounced dropdown; Enter navigates to a full results page). `TeamsPage` (`/teams?search=`) is the results page — big centered search bar, syncs its local state to the URL on re-search (same-route navigations don't remount, so a plain `useState` initializer alone would miss a second search). `TeamDetailPage` shows, top to bottom: live match banner (if any), upcoming matches, roster (split into "Starting lineup"/"Bench" by the backend's participation heuristic), "Results" (standings, full season tournament names, 🏆/🥈/🥉 by place), recent matches. Loads instantly from cached/DB data and shows a small "Updating matches…" spinner during the ~4s-delayed background refetch (see Current Progress: Team browser).
- **Live/watch:** `MatchCard` shows a self-adjusting countdown for upcoming matches (top-center); `StatusBadge` distinguishes match "LIVE" from tournament "ongoing" via a `kind` prop. `MatchDetailPage` shows `WatchLinks` (Twitch/YouTube/official site) for non-finished matches, sourced from the curated map in `leagueChannels.ts`.
- **Apex/ALGS:** "ALGS" nav item → `ApexPage` (`/apex`, match day list with region/status filters) and `ApexMatchDayDetailPage` (`/apex/:id`, ranked 20-team results; each row expands to a per-game placement/kills/points table — rows are clickable divs, not `<button>`s, because they embed the follow-star button and nested buttons are invalid HTML). Feed shows an "Upcoming ALGS match days" section only when non-empty. Apex team pages replace the LoL roster/matches sections with "Recent ALGS results" (Cito has no roster or head-to-head data). Settings league picker works for Apex unchanged (ALGS regions are Leagues).
- **Dev workflow:** `npm run dev` in `frontend/` (Vite dev server on `:5173`, proxying `/api` to `:8080` — see `vite.config.ts`); `npm run build` runs `tsc -b` (type-check) then `vite build`.

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
11. ✅ Implement user follows (games + teams). The earlier idea of making `RiotSyncService.syncMatches()` follow-driven was rejected in favor of keeping sync in-season-scoped and filtering per-user at read time (noted in Current Progress above)
12. ✅ Build personalized feed endpoint
13. ✅ Build "upcoming matches for my teams" endpoint
14. ✅ Write tests for personalization logic — 79 tests total (up from 64), plus manual end-to-end verification against real synced Riot data

### Phase 4 – Polish
15. ✅ done — filtering/pagination already built into the Tournament/Match endpoints (Phase 2 step 9), pulled forward rather than deferred
16. ✅ Add Swagger/OpenAPI documentation — `OpenApiConfig` sets title/version/description and declares a `bearer-jwt` security scheme; JWT-protected controllers (`UserController`, `FeedController`) get a class-level `@SecurityRequirement`, `MatchController.upcoming()` gets a method-level one; `SecurityConfig` permits `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, `/v3/api-docs/**`. UI at `/swagger-ui.html`, JSON at `/v3/api-docs`.
17. ✅ Add standings sync and endpoint — 83 tests total (up from 79), plus manual + live-IT verification against real Riot data (see Current Progress above)
18. ✅ done — `GlobalExceptionHandler` + RFC 7807 `ProblemDetail` responses have been in place since Phase 1, refined incrementally (404/409/401 handlers added as each feature needed them)
19. ✅ Write integration tests — repository tests for the three non-trivial custom queries (`MatchRepositoryTest`/`TournamentRepositoryTest`/`StandingRepositoryTest`, `@DataJpaTest` + H2) plus one full-stack `@SpringBootTest` (`FeedFlowIntegrationTest`) covering register → login → follow → feed. See Testing Strategy above. 88 tests total (up from 83).
20. ✅ Clean up README — full portfolio-oriented rewrite with Mermaid architecture diagram, tech-stack summary, Docker-based getting-started, curl walkthrough of the register → login → follow → feed flow, full endpoint reference table, testing overview, and a "notable design decisions" section linking back to CLAUDE.md

### Phase 5 – React Frontend ✅ done for the baseline described here (not in the original spec's phase list)
21. ✅ Scaffold Vite + React + TypeScript in `frontend/`, Tailwind CSS v4, TanStack Query, React Router; dev-server proxy to `:8080`
22. ✅ Auth: login/register pages, JWT in localStorage, `AuthContext`, `ProtectedRoute`
23. ✅ Public browsing: paginated matches/tournaments lists with filters, tournament detail with standings, today/upcoming views
24. ✅ Personalized feed page + Following/settings page (follow games for UI grouping, leagues + teams for feed content — collapsible International/Primary/National league picker)
25. ✅ Match details: `GET /api/matches/{id}/details` (Riot live-stats proxy, not synced — see Current Progress) + a per-game stats page with champion/item icons from Data Dragon
26. Manual/browser-preview verification only per feature — no automated frontend test suite (see Testing Strategy)

### Team Browser, Live Matches & Watch Links (2026-07-10, not in the original spec's phase list)
27. ✅ Team browser: `GET /api/teams` (search/browse) + `GET /api/teams/{id}` (detail), `Organization`/`Player` entities (migration V14), `Team.league` (migration V15), Riot `getTeams` sync, roster active/bench heuristic, throttled on-demand per-league sync from team pages. Frontend: nav search, `TeamsPage`, `TeamDetailPage`, `FollowTeamHeaderButton`.
28. ✅ Live-match visibility: `liveMatches` on `/api/feed` and team detail, "Live now" sections, match-vs-tournament `StatusBadge` distinction.
29. ✅ Watch links: `WatchLinks`/`leagueChannels.ts`, curated Twitch handles + universal YouTube/official-site links.
30. ✅ Backend test coverage for steps 27–28 closed 2026-07-10 (see Current Progress: team browser/live-match test coverage). Frontend is still manual/browser-preview verification only, same as the rest of Phase 5.

**Not yet done, deliberately deferred (see "Known gaps" in Current Progress):** Dota 2/Steam client (still fully unbuilt) — everything else previously listed here (React error boundary, follow-update race hardening, team browser/live-match test coverage) was closed 2026-07-10.

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
  cache:
    type: caffeine
    cache-names: matchDetails, leagueSyncThrottle
    caffeine:
      spec: expireAfterWrite=60s,maximumSize=200  # short-lived cache for match-details proxy + on-demand league sync throttle

riot:
  esports-api-key: ${RIOT_ESPORTS_API_KEY}
  base-url: https://esports-api.lolesports.com/persisted/gw
  feed-base-url: https://feed.lolesports.com/livestats/v1  # per-game live stats (champions/items/objectives), no API key needed

steam:
  api-key: ${STEAM_API_KEY}
  base-url: https://api.steampowered.com/IDOTA2Match_570

cito:
  api-key: ${CITO_API_KEY}
  base-url: https://api.citoapi.com/api/v1  # ALGS data; free tier 500 calls/month — see External API Integration

jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 86400000  # 24h

sync:
  matches-cron: "0 */15 * * * *"      # every 15 min
  tournaments-cron: "0 0 */6 * * *"   # every 6 hours
```

## External API Keys
- **Riot LoL Esports API:** no registration needed — uses the long-standing public key `0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z`, the same one lolesports.com's own frontend uses. Still kept in `RIOT_ESPORTS_API_KEY` rather than hardcoded, in case Riot ever rotates it.
- **Cito API (Apex/ALGS):** free personal key at https://citoapi.com/signup (`CITO_API_KEY`). 500 calls/month cap — the sync design assumes ~120/month; don't add call sites casually. The user's key lives in `.claude/launch.json` (gitignored), never in committed files.
- **Valve Steam Web API:** free personal key at https://steamcommunity.com/dev/apikey (requires a Steam account).

## Local Dev Environment
- Postgres runs in a Docker container named `esports-tracker-db` (Postgres 16, db `esportstracker`, user/password `esports`/`esports`, port 5432). Start it with `docker start esports-tracker-db` if stopped (check first: `docker ps -a --filter name=esports-tracker-db`) — it may stop if Docker Desktop restarts or the machine sleeps.
- Run the backend: `./mvnw spring-boot:run` with `DB_USERNAME`, `DB_PASSWORD`, `RIOT_ESPORTS_API_KEY`, `CITO_API_KEY`, `STEAM_API_KEY` (placeholder fine, unused until Dota 2 is built), `JWT_SECRET` set as env vars — see README.md and `.claude/launch.json`.
- Run the frontend: `npm run dev` in `frontend/` (needs Node.js LTS installed — not preinstalled on a fresh machine) → `http://localhost:5173`, proxying `/api` calls to the backend on `:8080`. Both must be running for the frontend to show real data.
- Static test console at `http://localhost:8080/` (register/login/me + public games list) — see `src/main/resources/static/index.html`. Superseded by the React frontend for anything beyond a quick manual API smoke-check.

## Git Strategy
- Main branch: `main` (always deployable)
- Feature branches: `feature/entity-name` or `feature/endpoint-name`
- Commit messages: conventional commits (`feat:`, `fix:`, `test:`, `docs:`)
- .gitignore: IDE files, build output, .env, application-local.yml
