package dev.mundorf.esportstracker.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the Caffeine-backed cache declared in application.yml (spring.cache.*).
 * Currently used only for the on-demand match details proxy: details are fetched
 * from Riot at request time rather than synced, so the cache is what stops a
 * popular live match from turning every page view into a burst of Riot calls.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
