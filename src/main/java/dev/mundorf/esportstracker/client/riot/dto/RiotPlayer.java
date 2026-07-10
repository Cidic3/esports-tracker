package dev.mundorf.esportstracker.client.riot.dto;

public record RiotPlayer(
        String id,
        String summonerName,
        String firstName,
        String lastName,
        String image,
        String role
) {
}
