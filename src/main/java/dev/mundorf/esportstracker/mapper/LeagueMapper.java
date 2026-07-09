package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.LeagueResponse;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import org.springframework.stereotype.Component;

@Component
public class LeagueMapper {

    public LeagueResponse toResponse(League league) {
        return new LeagueResponse(
                league.getId(),
                league.getName(),
                league.getSlug(),
                league.getRegion(),
                TournamentTier.forLeague(league.getRegion(), league.getSlug()));
    }
}
