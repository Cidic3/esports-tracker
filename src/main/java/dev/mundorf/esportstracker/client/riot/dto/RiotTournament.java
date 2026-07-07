package dev.mundorf.esportstracker.client.riot.dto;

import java.time.LocalDate;

public record RiotTournament(String id, String slug, LocalDate startDate, LocalDate endDate) {
}
