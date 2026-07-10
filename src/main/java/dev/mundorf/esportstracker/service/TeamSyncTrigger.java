package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ExternalApiException;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.service.sync.RiotSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Fire-and-forget wrapper around {@link RiotSyncService#syncLeagueOnDemand}, called from
 * TeamService.findById when a team page loads. Separate bean (not a method on TeamService itself)
 * because {@code @Async} is applied via a Spring AOP proxy - a self-invoked call from within
 * TeamService would bypass that proxy and run synchronously, defeating the point. The page must
 * render immediately from whatever's already in the DB; this sync just makes the next visit fresher.
 */
@Component
public class TeamSyncTrigger {

    private static final Logger log = LoggerFactory.getLogger(TeamSyncTrigger.class);

    private final RiotSyncService riotSyncService;

    public TeamSyncTrigger(RiotSyncService riotSyncService) {
        this.riotSyncService = riotSyncService;
    }

    @Async
    public void triggerLeagueSync(League league) {
        try {
            riotSyncService.syncLeagueOnDemand(league);
        } catch (ExternalApiException ex) {
            log.warn("Background on-demand sync failed for league {}: {}", league.getSlug(), ex.toString());
        }
    }
}
