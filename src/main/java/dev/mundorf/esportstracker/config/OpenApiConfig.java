package dev.mundorf.esportstracker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the OpenAPI document that springdoc-openapi-starter-webmvc-ui exposes at
 * {@code /v3/api-docs} and renders at {@code /swagger-ui.html}. Kept as a pure annotation-only
 * config (no @Bean methods) since springdoc auto-discovers @OpenAPIDefinition / @SecurityScheme
 * on any @Configuration class.
 *
 * <p>The bearer security scheme is declared here but not applied globally - most endpoints
 * (games/tournaments/matches listing) are public, so we opt individual JWT-protected endpoints
 * in via {@code @SecurityRequirement(name = "bearer-jwt")}. That way Swagger UI shows a lock
 * icon only on the endpoints that actually need one.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Esports Tracker API",
                version = "0.0.1",
                description = """
                        REST API for tracking League of Legends esports: browse tournaments, matches, and
                        standings synced from Riot's public esports API, or register and follow specific
                        games/teams for a personalized feed. Portfolio project - see the repo README for
                        the architecture overview, and CLAUDE.md for the design decisions behind it.
                        """))
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the token returned by POST /api/auth/login (no \"Bearer \" prefix - Swagger adds it).")
public class OpenApiConfig {
}
