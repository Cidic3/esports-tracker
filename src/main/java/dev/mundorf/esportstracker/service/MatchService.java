package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Match;
import dev.mundorf.esportstracker.repository.MatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class MatchService {

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public Page<Match> search(String gameSlug, EventStatus status, UUID teamId,
                              Instant from, Instant to, Pageable pageable) {
        return matchRepository.search(gameSlug, status, teamId, from, to, pageable);
    }

    public Page<Match> findByTournament(UUID tournamentId, Pageable pageable) {
        return matchRepository.findByTournamentId(tournamentId, pageable);
    }

    public Match findById(UUID id) {
        return matchRepository.findWithAssociationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }

    /** Matches scheduled anytime during the current UTC calendar day, in chronological order. */
    public List<Match> findToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = today.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        return matchRepository.findByScheduledAtBetweenOrderByScheduledAtAsc(startOfDay, endOfDay);
    }
}
