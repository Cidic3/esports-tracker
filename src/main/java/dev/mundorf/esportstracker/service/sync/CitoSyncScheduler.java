package dev.mundorf.esportstracker.service.sync;

import dev.mundorf.esportstracker.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Thin cron layer over {@link CitoSyncService}, same split as RiotSyncScheduler. Runs ONLY on the
 * slow tournaments-cron (6h): each tick costs exactly one Cito API call, and the free tier allows
 * 500 calls/month - the 15-min matches-cron would burn through that in under three days.
 */
@Component
public class CitoSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(CitoSyncScheduler.class);

    private final CitoSyncService syncService;

    public CitoSyncScheduler(CitoSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "${sync.tournaments-cron}")
    public void scheduledAlgsSync() {
        try {
            syncService.syncAlgsEvents();
        } catch (ExternalApiException ex) {
            log.error("Scheduled ALGS sync failed", ex);
        }
    }
}
