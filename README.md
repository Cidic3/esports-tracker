# Esports Tracker

Personal esports tracking platform - a portfolio project demonstrating Spring Boot, REST API
design, relational database modeling, external API integration, and testing. See
[CLAUDE.md](CLAUDE.md) for the full project specification and build plan.

## Prerequisites

- Java 21 (JDK)
- PostgreSQL running locally (or via Docker)
- A free [Steam Web API key](https://steamcommunity.com/dev/apikey) (for Dota 2 data)

No local Maven install is required - this project uses the Maven Wrapper.

## Running

```bash
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export RIOT_ESPORTS_API_KEY=0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z
export STEAM_API_KEY=your_steam_api_key
export JWT_SECRET=a-long-random-secret-string

./mvnw spring-boot:run
```

On Windows (PowerShell): use `$env:VAR = "value"` instead of `export`.
