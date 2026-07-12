package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.ApexMatchDayRepository;
import dev.mundorf.esportstracker.repository.ApexTeamResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApexMatchDayServiceTest {

    @Mock
    private ApexMatchDayRepository matchDayRepository;
    @Mock
    private ApexTeamResultRepository teamResultRepository;

    @InjectMocks
    private ApexMatchDayService apexMatchDayService;

    private final Game apexGame = withId(new Game("Apex Legends", "apex-legends", null));
    private final Game lolGame = withId(new Game("League of Legends", "league-of-legends", null));

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    @Test
    void shouldReturnEmptyPageWithoutQueryingWhenUserFollowsNothing() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        Pageable pageable = PageRequest.of(0, 20);

        Page<?> result = apexMatchDayService.findUpcomingForUser(user, pageable);

        assertThat(result).isEmpty();
        verify(matchDayRepository, never()).findUpcomingForLeagues(any(), any());
    }

    @Test
    void shouldQueryByFollowedLeagueIds() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        League algsEmea = withId(new League("ALGS EMEA", "algs-emea", "EMEA", apexGame, "algs-emea"));
        user.replaceFollowedLeagues(Set.of(algsEmea));
        Pageable pageable = PageRequest.of(0, 20);
        when(matchDayRepository.findUpcomingForLeagues(any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        apexMatchDayService.findUpcomingForUser(user, pageable);

        ArgumentCaptor<Set<UUID>> captor = ArgumentCaptor.forClass(Set.class);
        verify(matchDayRepository).findUpcomingForLeagues(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).containsExactly(algsEmea.getId());
    }

    @Test
    void shouldResolveFollowedApexTeamToItsHomeLeague() {
        // A pending BR match day has no team list, so following an Apex team implies following
        // its ALGS region's schedule - see the javadoc on findUpcomingForUser.
        User user = new User("testuser", "test@example.com", "hashed-password");
        League algsAmericas = withId(new League("ALGS Americas", "algs-americas", "AMERICAS", apexGame, "algs-americas"));
        Team apexTeam = withId(new Team("VK GAMING", "vk-gaming", null, apexGame, "vk-gaming"));
        apexTeam.assignLeague(algsAmericas);
        user.replaceFollowedTeams(Set.of(apexTeam));
        Pageable pageable = PageRequest.of(0, 20);
        when(matchDayRepository.findUpcomingForLeagues(any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        apexMatchDayService.findUpcomingForUser(user, pageable);

        ArgumentCaptor<Set<UUID>> captor = ArgumentCaptor.forClass(Set.class);
        verify(matchDayRepository).findUpcomingForLeagues(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).containsExactly(algsAmericas.getId());
    }

    @Test
    void shouldNotWidenQueryForFollowedLolTeams() {
        // The team->league widening is Apex-only; a followed LoL team must NOT drag its whole
        // league into the apex feed section (or anywhere else - LoL follows keep their semantics).
        User user = new User("testuser", "test@example.com", "hashed-password");
        League lec = withId(new League("LEC", "lec", "EMEA", lolGame, "L1"));
        Team g2 = withId(new Team("G2 Esports", "g2-esports", null, lolGame, "TA"));
        g2.assignLeague(lec);
        user.replaceFollowedTeams(Set.of(g2));
        Pageable pageable = PageRequest.of(0, 20);

        Page<?> result = apexMatchDayService.findUpcomingForUser(user, pageable);

        assertThat(result).isEmpty();
        verify(matchDayRepository, never()).findUpcomingForLeagues(any(), any());
    }

    @Test
    void shouldIgnoreFollowedApexTeamWithoutHomeLeague() {
        User user = new User("testuser", "test@example.com", "hashed-password");
        Team orphan = withId(new Team("No League Team", "no-league-team", null, apexGame, "no-league-team"));
        user.replaceFollowedTeams(Set.of(orphan));
        Pageable pageable = PageRequest.of(0, 20);

        Page<?> result = apexMatchDayService.findUpcomingForUser(user, pageable);

        assertThat(result).isEmpty();
        verify(matchDayRepository, never()).findUpcomingForLeagues(any(), any());
    }
}
