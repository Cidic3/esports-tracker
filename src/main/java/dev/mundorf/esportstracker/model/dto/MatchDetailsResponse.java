package dev.mundorf.esportstracker.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * Per-game breakdown of a best-of series, proxied from Riot's live stats feed on demand
 * (never persisted — see MatchDetailsService). Games without stats coverage still appear,
 * with null team blocks, so the client can render "game 3: no data" rather than a gap.
 *
 * <p>champion is a Data Dragon champion key ("DrMundo"); items are Data Dragon item ids.
 * The client resolves both to images via Riot's public CDN — we deliberately don't proxy
 * static assets through our API.
 */
public record MatchDetailsResponse(UUID matchId, List<GameDetails> games) {

    public record GameDetails(int number, String state, TeamGameDetails blueTeam, TeamGameDetails redTeam) {
    }

    /**
     * teamId/name are our own Team identity (resolved from Riot's side assignment via
     * externalId), nullable if Riot reports a side we can't match to the series' teams.
     */
    public record TeamGameDetails(
            UUID teamId,
            String name,
            int totalKills,
            int totalGold,
            int towers,
            int barons,
            List<String> dragons,
            List<PlayerGameDetails> players) {
    }

    public record PlayerGameDetails(
            String summonerName,
            String champion,
            String role,
            int level,
            int kills,
            int deaths,
            int assists,
            int creepScore,
            long gold,
            List<Integer> items) {
    }
}
