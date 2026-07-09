package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.TournamentResponse;
import dev.mundorf.esportstracker.model.entity.Tournament;
import org.springframework.stereotype.Component;

@Component
public class TournamentMapper {

    private final LeagueMapper leagueMapper;

    public TournamentMapper(LeagueMapper leagueMapper) {
        this.leagueMapper = leagueMapper;
    }

    public TournamentResponse toResponse(Tournament tournament) {
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
                leagueMapper.toResponse(tournament.getLeague()));
    }
}
