package dev.mundorf.esportstracker.client.riot.dto;

/**
 * One team's row within a ranked section of a tournament's standings (already flattened out of
 * Riot's stage/section/ranking nesting - see {@link dev.mundorf.esportstracker.client.riot.RiotEsportsClient#getStandings}).
 * {@code team} reuses {@link RiotEventTeam} since the fields Riot exposes here (id/name/code/image)
 * are identical; {@code gameWins} on it is unused and left null.
 */
public record RiotStandingEntry(String groupName, int rank, RiotEventTeam team, int wins, int losses) {
}
