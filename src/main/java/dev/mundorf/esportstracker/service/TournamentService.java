package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Team;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.model.entity.User;
import dev.mundorf.esportstracker.repository.TournamentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TournamentService {

    // Never a real UUID (Riot/Valve external data + JPA @GeneratedValue(UUID) both produce random
    // v4 UUIDs), used to keep an "IN" clause non-empty when a user follows nothing of that kind.
    private static final Set<UUID> NONE = Set.of(new UUID(0, 0));

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    public Page<Tournament> search(String gameSlug, EventStatus status, TournamentTier tier, Pageable pageable) {
        return tournamentRepository.search(gameSlug, status, tier, pageable);
    }

    public Tournament findById(UUID id) {
        return tournamentRepository.findWithAssociationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
    }

    /** Running tournaments for a user's followed leagues/teams, most recently started first, capped to {@code limit}. */
    public List<Tournament> findRunningForUser(User user, int limit) {
        Set<UUID> leagueIds = user.getFollowedLeagues().stream().map(League::getId).collect(Collectors.toSet());
        Set<UUID> teamIds = user.getFollowedTeams().stream().map(Team::getId).collect(Collectors.toSet());
        if (leagueIds.isEmpty() && teamIds.isEmpty()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startDate"));
        return tournamentRepository.findRunningForFollowed(
                leagueIds.isEmpty() ? NONE : leagueIds,
                teamIds.isEmpty() ? NONE : teamIds,
                pageable);
    }
}
