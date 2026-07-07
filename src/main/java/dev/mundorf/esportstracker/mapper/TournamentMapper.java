package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.LeagueResponse;
import dev.mundorf.esportstracker.model.dto.TournamentResponse;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Tournament;
import org.springframework.stereotype.Component;

@Component
public class TournamentMapper {

    public TournamentResponse toResponse(Tournament tournament) {
        League league = tournament.getLeague();
        LeagueResponse leagueResponse = new LeagueResponse(
                league.getId(), league.getName(), league.getSlug(), league.getRegion());

        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getSlug(),
                tournament.getTier(),
                tournament.getStatus(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getPrizePool(),
                tournament.getGame().getSlug(),
                leagueResponse);
    }
}
