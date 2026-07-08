package dev.mundorf.esportstracker.service.sync;

import dev.mundorf.esportstracker.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Thin cron layer over {@link RiotSyncService}. Kept separate from the service so the sync logic
 * can be unit/integration-tested by calling it directly, without waiting on a cron trigger.
 * External-API failures are caught and logged here so a bad poll never propagates out of the
 * scheduler thread.
 */
@Component
public class RiotSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(RiotSyncScheduler.class);

    private final RiotSyncService syncService;

    public RiotSyncScheduler(RiotSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "${sync.tournaments-cron}")
    public void scheduledLeagueAndTournamentSync() {
        try {
            syncService.syncLeaguesAndTournaments();
        } catch (ExternalApiException ex) {
            log.error("Scheduled league/tournament sync failed", ex);
        }
    }

    @Scheduled(cron = "${sync.matches-cron}")
    public void scheduledMatchSync() {
        try {
            syncService.syncMatches();
        } catch (ExternalApiException ex) {
            log.error("Scheduled match sync failed", ex);
        }
    }

    // Standings only change once a match completes, so this piggybacks on the same cadence and
    // "in-season" scope as the match sync above rather than running on its own schedule.
    @Scheduled(cron = "${sync.matches-cron}")
    public void scheduledStandingsSync() {
        try {
            syncService.syncStandings();
        } catch (ExternalApiException ex) {
            log.error("Scheduled standings sync failed", ex);
        }
    }
}
