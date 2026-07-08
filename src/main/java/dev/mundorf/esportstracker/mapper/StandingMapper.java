package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.StandingResponse;
import dev.mundorf.esportstracker.model.entity.Standing;
import org.springframework.stereotype.Component;

@Component
public class StandingMapper {

    private final TeamMapper teamMapper;

    public StandingMapper(TeamMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    public StandingResponse toResponse(Standing standing) {
        return new StandingResponse(
                standing.getGroupName(),
                standing.getRank(),
                standing.getWins(),
                standing.getLosses(),
                teamMapper.toResponse(standing.getTeam()));
    }
}
