package dev.mundorf.esportstracker.client.riot.dto;

import java.util.List;

/**
 * Per-player stats from the livestats "details" endpoint's most recent frame.
 * Items are numeric Data Dragon item ids; participantId joins back to
 * {@link RiotGameWindow.RiotParticipantMeta} (1–5 blue side, 6–10 red side).
 */
public record RiotParticipantStats(
        int participantId,
        int level,
        int kills,
        int deaths,
        int assists,
        int creepScore,
        long totalGoldEarned,
        List<Integer> items) {
}
