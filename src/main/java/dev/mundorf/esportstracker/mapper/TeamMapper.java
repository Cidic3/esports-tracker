package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.TeamResponse;
import dev.mundorf.esportstracker.model.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getSlug(), team.getLogoUrl());
    }
}
