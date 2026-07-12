package dev.mundorf.esportstracker.model.dto;

/** One team's result in a single game of an ALGS match day. */
public record ApexGameResultResponse(int gameNumber, int placement, int kills, int points) {
}
