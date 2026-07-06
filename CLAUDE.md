# Esports Tracker – Project Specification

## Overview
A personal esports tracking platform where users select which games and teams they follow, and get a unified dashboard of upcoming tournaments, match scores, schedules, and standings – across multiple esports titles.

**Purpose:** Portfolio project to demonstrate Spring Boot, REST API design, relational database modeling, external API integration, and testing. Built by a CS student (graduating Sept 2026) to strengthen backend skills for job applications.

**No AI/LLM features.** This is a pure backend project.

## Working With Claude
The user is building this project to learn, not just to have it built. In every session:
- Always apply industry best practices for the relevant layer (Spring Boot, JPA, REST API design, security, testing) rather than the shortest path to something that runs.
- Explain the *why* behind architectural and implementation decisions as you make them — what problem the pattern solves, what tradeoffs exist, and why it fits here specifically (not generic textbook explanations).
- When there are multiple valid approaches, briefly mention the alternative(s) and why the chosen one wins in this context.
- Favor teaching moments over silently doing the "correct" thing — assume the user wants to be able to explain these decisions themselves later (e.g. in interviews).

## Tech Stack
- **Framework:** Spring Boot 3.x (Java 21)
- **Build Tool:** Maven
- **Database:** PostgreSQL (with Spring Data JPA / Hibernate)
- **API Documentation:** SpringDoc OpenAPI (Swagger UI)
- **Testing:** JUnit 5 + Mockito + Spring Boot Test
- **External Data Source:** PandaScore API (free tier – covers LoL, CS2, Dota 2, Valorant, and more)
- **Migration:** Flyway for database versioning
- **Optional Frontend:** None initially. The project is API-first – all functionality exposed via REST endpoints. A frontend (React or Thymeleaf) can be added later.

## Architecture

### Layered Structure
```
src/main/java/dev/mundorf/esportstracker/
├── controller/        # REST controllers
├── service/           # Business logic
├── repository/        # Spring Data JPA repositories
├── model/
│   ├── entity/        # JPA entities
│   └── dto/           # Request/Response DTOs
├── client/            # External API clients (PandaScore)
├── config/            # Spring configuration, CORS, Scheduler
├── exception/         # Custom exceptions + global handler
└── mapper/            # Entity <-> DTO mapping
```

### Core Entities

**User**
- id, username, email, passwordHash, createdAt
- Follows games (many-to-many)
- Follows teams (many-to-many)

**Game**
- id, name, slug (e.g. "league-of-legends", "cs2"), iconUrl
- Source: seeded from PandaScore

**Team**
- id, name, slug, logoUrl, game (many-to-one)
- Source: synced from PandaScore

**Tournament**
- id, name, slug, game (many-to-one), startDate, endDate, tier (S/A/B/C), status (upcoming/running/finished), prizePool
- Source: synced from PandaScore

**Match**
- id, tournament (many-to-one), game (many-to-one), teamA (many-to-one), teamB (many-to-one), scheduledAt, status (upcoming/running/finished), scoreA, scoreB, streamUrl
- Source: synced from PandaScore

**Standing** (per tournament)
- id, tournament (many-to-one), team (many-to-one), rank, wins, losses, draws

### REST Endpoints

**Auth & Users**
```
POST   /api/auth/register          – Register new user
POST   /api/auth/login             – Login (returns JWT)
GET    /api/users/me               – Get current user profile
PUT    /api/users/me/games         – Update followed games
PUT    /api/users/me/teams         – Update followed teams
```

**Games**
```
GET    /api/games                  – List all supported games
GET    /api/games/{slug}           – Get game details
GET    /api/games/{slug}/teams     – List teams for a game
GET    /api/games/{slug}/tournaments – List tournaments for a game
```

**Tournaments**
```
GET    /api/tournaments            – List tournaments (filter by game, status, tier)
GET    /api/tournaments/{id}       – Tournament details with standings
GET    /api/tournaments/{id}/matches – Matches in a tournament
```

**Matches**
```
GET    /api/matches                – List matches (filter by game, team, status, date range)
GET    /api/matches/upcoming       – Upcoming matches for followed games/teams (auth required)
GET    /api/matches/today          – Today's matches across all games
GET    /api/matches/{id}           – Match details
```

**Feed (personalized)**
```
GET    /api/feed                   – Combined feed: upcoming matches + running tournaments for followed games/teams (auth required)
```

### External API Integration – PandaScore

**Base URL:** `https://api.pandascore.co/`
**Auth:** Bearer token (API key from free tier)
**Rate Limit:** 1 request/second on free tier

Key endpoints to consume:
- `GET /videogames` – list supported games
- `GET /{game}/teams` – teams per game
- `GET /{game}/tournaments` – tournaments per game
- `GET /{game}/matches` – matches per game (upcoming, running, past)
- `GET /tournaments/{id}/standings` – standings

**Sync Strategy:**
- Use `@Scheduled` Spring tasks to periodically poll PandaScore
- Tournaments/teams: sync every 6 hours
- Matches (upcoming/running): sync every 15 minutes
- Store PandaScore IDs alongside internal IDs for deduplication
- Never expose PandaScore data directly – always map to internal entities

### Database Schema Notes
- All entities use auto-generated UUIDs as primary keys
- Store `pandascoreId` (Long) on Game, Team, Tournament, Match for sync deduplication
- Use `@CreatedDate` and `@LastModifiedDate` on all entities
- Flyway migrations in `src/main/resources/db/migration/`
- Index: match.scheduledAt, match.status, tournament.status, team.game_id

### Testing Strategy
- **Unit tests:** Service layer with mocked repositories
- **Integration tests:** Controller layer with `@WebMvcTest` and mocked services
- **Repository tests:** With `@DataJpaTest` and embedded H2 or Testcontainers (PostgreSQL)
- Target: every service method has at least one happy-path and one edge-case test
- Use meaningful test names: `shouldReturnUpcomingMatchesForFollowedTeams()`

### Security
- JWT-based authentication (Spring Security + jjwt)
- Public endpoints: game listing, tournament listing, match listing, registration, login
- Protected endpoints: feed, user profile, follow/unfollow
- Passwords hashed with BCrypt

## Build Order (incremental)

### Phase 1 – Foundation
1. Initialize Spring Boot project with dependencies (Web, JPA, PostgreSQL, Security, Validation, OpenAPI)
2. Set up Flyway + first migration (User, Game tables)
3. Implement Game entity, repository, service, controller
4. Implement User registration + JWT auth
5. Write tests for Game service and Auth flow

### Phase 2 – Core Data
6. Add Team, Tournament, Match entities + migrations
7. Implement PandaScore client (RestTemplate or WebClient)
8. Build sync scheduler for games and teams
9. Implement Tournament and Match endpoints
10. Write tests for sync logic and endpoints

### Phase 3 – Personalization
11. Implement user follows (games + teams)
12. Build personalized feed endpoint
13. Build "upcoming matches for my teams" endpoint
14. Write tests for personalization logic

### Phase 4 – Polish
15. Add filtering/pagination to list endpoints
16. Add Swagger/OpenAPI documentation
17. Add standings sync and endpoint
18. Refine error handling (global exception handler, consistent error responses)
19. Write integration tests
20. Clean up README with setup instructions, API examples, architecture diagram

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
  flyway:
    enabled: true

pandascore:
  api-key: ${PANDASCORE_API_KEY}
  base-url: https://api.pandascore.co

jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 86400000  # 24h

sync:
  matches-cron: "0 */15 * * * *"      # every 15 min
  tournaments-cron: "0 0 */6 * * *"   # every 6 hours
```

## PandaScore API Key
Register for free at https://pandascore.co/ – the free tier gives 1000 requests/hour, more than enough for development and a portfolio demo.

## Git Strategy
- Main branch: `main` (always deployable)
- Feature branches: `feature/entity-name` or `feature/endpoint-name`
- Commit messages: conventional commits (`feat:`, `fix:`, `test:`, `docs:`)
- .gitignore: IDE files, build output, .env, application-local.yml
