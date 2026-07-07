package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.MatchResponse;
import dev.mundorf.esportstracker.model.entity.Match;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    private final TeamMapper teamMapper;

    public MatchMapper(TeamMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    public MatchResponse toResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getScheduledAt(),
                match.getStatus(),
                match.getScoreA(),
                match.getScoreB(),
                match.getStreamUrl(),
                teamMapper.toResponse(match.getTeamA()),
                teamMapper.toResponse(match.getTeamB()),
                match.getTournament().getId(),
                match.getTournament().getName(),
                match.getGame().getSlug());
    }
}
