package dev.mundorf.esportstracker.client.riot.dto;

/** From Riot's getTeams endpoint - the league a team currently competes in. Name-only, no id. */
public record RiotHomeLeague(String name, String region) {
}
