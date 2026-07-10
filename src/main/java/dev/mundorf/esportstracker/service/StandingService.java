package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.model.entity.Standing;
import dev.mundorf.esportstracker.repository.StandingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StandingService {

    private final StandingRepository standingRepository;

    public StandingService(StandingRepository standingRepository) {
        this.standingRepository = standingRepository;
    }

    /** A tournament's standings, grouped and ordered by group name then rank. */
    public List<Standing> findByTournament(UUID tournamentId) {
        return standingRepository.findByTournamentIdOrderByGroupNameAscRankAsc(tournamentId);
    }

    /** Every tournament a team currently has a ranked position in (usually just its active split). */
    public List<Standing> findByTeam(UUID teamId) {
        return standingRepository.findByTeamId(teamId);
    }
}
