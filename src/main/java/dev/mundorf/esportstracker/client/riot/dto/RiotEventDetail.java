package dev.mundorf.esportstracker.client.riot.dto;

import java.util.List;

/**
 * Flattened result of getEventDetails - unlike getSchedule, this exposes the authoritative
 * tournament id a match belongs to, plus stable team ids (getSchedule only gives team names).
 */
public record RiotEventDetail(String matchId, String tournamentId, List<RiotEventTeam> teams) {
}
