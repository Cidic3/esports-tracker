package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ExternalApiException;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.service.sync.RiotSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeamSyncTriggerTest {

    @Mock
    private RiotSyncService riotSyncService;

    @InjectMocks
    private TeamSyncTrigger teamSyncTrigger;

    private final Game lolGame = new Game("League of Legends", "league-of-legends", null);
    private final League lec = new League("LEC", "lec", "EMEA", lolGame, "L1");

    @Test
    void shouldDelegateToOnDemandSync() {
        teamSyncTrigger.triggerLeagueSync(lec);

        verify(riotSyncService).syncLeagueOnDemand(lec);
    }

    @Test
    void shouldSwallowExternalApiExceptionRatherThanPropagating() {
        doThrow(new ExternalApiException("Riot unavailable", new RuntimeException("timeout")))
                .when(riotSyncService).syncLeagueOnDemand(lec);

        // Should not throw - a background sync failure must never surface to the page load that triggered it.
        teamSyncTrigger.triggerLeagueSync(lec);

        verify(riotSyncService).syncLeagueOnDemand(lec);
    }
}
