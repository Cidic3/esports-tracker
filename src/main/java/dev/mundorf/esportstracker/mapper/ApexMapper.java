package dev.mundorf.esportstracker.mapper;

import dev.mundorf.esportstracker.model.dto.ApexGameResultResponse;
import dev.mundorf.esportstracker.model.dto.ApexMatchDayDetailResponse;
import dev.mundorf.esportstracker.model.dto.ApexMatchDayResponse;
import dev.mundorf.esportstracker.model.dto.ApexTeamRecentResultResponse;
import dev.mundorf.esportstracker.model.dto.ApexTeamResultResponse;
import dev.mundorf.esportstracker.model.entity.ApexGameResult;
import dev.mundorf.esportstracker.model.entity.ApexMatchDay;
import dev.mundorf.esportstracker.model.entity.ApexTeamResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ApexMapper {

    private final TeamMapper teamMapper;

    public ApexMapper(TeamMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    public ApexMatchDayResponse toResponse(ApexMatchDay day) {
        return new ApexMatchDayResponse(
                day.getId(),
                day.getName(),
                day.getStartsAt(),
                day.getStatus(),
                day.getTournament().getId(),
                day.getTournament().getName(),
                day.getTournament().getLeague().getSlug(),
                day.getTournament().getLeague().getName(),
                day.getGame().getSlug());
    }

    /**
     * Accepts pre-assembled result rows rather than fetching here - the controller composes
     * results + game breakdowns from the repositories, same layering as TeamMapper.toDetailResponse.
     */
    public ApexMatchDayDetailResponse toDetailResponse(ApexMatchDay day, List<ApexTeamResultResponse> results) {
        return new ApexMatchDayDetailResponse(
                day.getId(),
                day.getName(),
                day.getStartsAt(),
                day.getStatus(),
                day.getTournament().getId(),
                day.getTournament().getName(),
                day.getTournament().getLeague().getSlug(),
                day.getTournament().getLeague().getName(),
                day.getGame().getSlug(),
                results);
    }

    public ApexTeamResultResponse toResultResponse(ApexTeamResult result, List<ApexGameResult> games) {
        List<ApexGameResultResponse> gameResponses = games.stream()
                .sorted(Comparator.comparingInt(ApexGameResult::getGameNumber))
                .map(g -> new ApexGameResultResponse(g.getGameNumber(), g.getPlacement(), g.getKills(), g.getPoints()))
                .toList();
        return new ApexTeamResultResponse(
                result.getRank(), result.getTotalPoints(), teamMapper.toResponse(result.getTeam()), gameResponses);
    }

    public ApexTeamRecentResultResponse toRecentResultResponse(ApexTeamResult result) {
        ApexMatchDay day = result.getMatchDay();
        return new ApexTeamRecentResultResponse(
                day.getId(),
                day.getName(),
                day.getStartsAt(),
                day.getTournament().getName(),
                result.getRank(),
                result.getTotalPoints());
    }
}
