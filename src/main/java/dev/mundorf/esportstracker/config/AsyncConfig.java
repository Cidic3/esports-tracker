package dev.mundorf.esportstracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables {@code @Async} (picked up by Spring Boot's auto-configured ThreadPoolTaskExecutor).
 * Currently used only to fire the on-demand, team-page-triggered league sync in the background
 * (see TeamSyncTrigger) - a team page must render immediately from whatever's already in the DB,
 * never block on a Riot round-trip.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
