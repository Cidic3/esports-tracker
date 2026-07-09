package dev.mundorf.esportstracker.client.riot.dto;

/**
 * One game of a best-of series, from getEventDetails' games array.
 * State is Riot's vocabulary: "completed", "inProgress", or "unstarted".
 */
public record RiotEventGame(String id, int number, String state, String blueTeamId, String redTeamId) {
}
