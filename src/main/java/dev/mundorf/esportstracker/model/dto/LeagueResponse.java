package dev.mundorf.esportstracker.model.dto;

import java.util.UUID;

public record LeagueResponse(UUID id, String name, String slug, String region) {
}
