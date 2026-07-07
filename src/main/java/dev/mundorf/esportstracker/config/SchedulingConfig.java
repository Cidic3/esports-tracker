package dev.mundorf.esportstracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on Spring's @Scheduled support. Cron jobs are registered at startup but do NOT fire on
 * boot - so restarting the app doesn't hammer the external APIs, they only run on their cron cadence.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
