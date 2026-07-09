package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.repository.LeagueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeagueService {

    private final LeagueRepository leagueRepository;

    public LeagueService(LeagueRepository leagueRepository) {
        this.leagueRepository = leagueRepository;
    }

    /**
     * Flat, unpaginated list (~40 leagues per game) — deliberately not a PagedResponse;
     * like standings, the collection is small and the settings UI wants it whole.
     */
    public List<League> findAll(String gameSlug) {
        if (gameSlug == null) {
            return leagueRepository.findAllByOrderByRegionAscNameAsc();
        }
        return leagueRepository.findByGameSlugOrderByRegionAscNameAsc(gameSlug);
    }
}
