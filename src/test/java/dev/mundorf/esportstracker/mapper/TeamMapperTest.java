package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.TeamDetailResponse;
import dev.mundorf.esportstracker.model.dto.TeamResponse;
import dev.mundorf.esportstracker.model.dto.TeamSummaryResponse;
import dev.mundorf.esportstracker.model.entity.Game;
import dev.mundorf.esportstracker.model.entity.Organization;
import dev.mundorf.esportstracker.model.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TeamMapperTest {

    private final TeamMapper mapper = new TeamMapper();
    private final Game lolGame = new Game("League of Legends", "league-of-legends", null);

    @Test
    void shouldMapTeamToResponse() {
        Team team = new Team("G2 Esports", "g2-esports", "logo.png", lolGame, "TA");

        TeamResponse response = mapper.toResponse(team);

        assertThat(response.name()).isEqualTo("G2 Esports");
        assertThat(response.slug()).isEqualTo("g2-esports");
        assertThat(response.logoUrl()).isEqualTo("logo.png");
    }

    @Test
    void shouldMapTeamToSummaryResponseIncludingGameSlug() {
        Team team = new Team("G2 Esports", "g2-esports", "logo.png", lolGame, "TA");

        TeamSummaryResponse response = mapper.toSummaryResponse(team);

        assertThat(response.gameSlug()).isEqualTo("league-of-legends");
        assertThat(response.name()).isEqualTo("G2 Esports");
    }

    @Test
    void shouldMapOrganizationWhenPresent() {
        Team team = new Team("G2 Esports", "g2-esports", "logo.png", lolGame, "TA");
        Organization org = new Organization("G2 Esports", "g2", "org-logo.png");
        team.assignOrganization(org);

        TeamDetailResponse response = mapper.toDetailResponse(
                team, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(response.organization()).isNotNull();
        assertThat(response.organization().name()).isEqualTo("G2 Esports");
        assertThat(response.organization().slug()).isEqualTo("g2");
    }

    @Test
    void shouldReturnNullOrganizationWhenTeamHasNone() {
        Team team = new Team("G2 Esports", "g2-esports", "logo.png", lolGame, "TA");

        TeamDetailResponse response = mapper.toDetailResponse(
                team, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(response.organization()).isNull();
    }
}
