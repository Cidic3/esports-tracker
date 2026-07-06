# Esports Tracker

Personal esports tracking platform - a portfolio project demonstrating Spring Boot, REST API
design, relational database modeling, external API integration, and testing. See
[CLAUDE.md](CLAUDE.md) for the full project specification and build plan.

## Prerequisites

- Java 21 (JDK)
- PostgreSQL running locally (or via Docker)
- A [PandaScore](https://pandascore.co/) API key (free tier)

No local Maven install is required - this project uses the Maven Wrapper.

## Running

```bash
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export PANDASCORE_API_KEY=your_pandascore_key
export JWT_SECRET=a-long-random-secret-string

./mvnw spring-boot:run
```

On Windows (PowerShell): use `$env:VAR = "value"` instead of `export`.
