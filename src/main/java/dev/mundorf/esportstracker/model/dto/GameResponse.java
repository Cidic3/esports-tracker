package dev.mundorf.esportstracker.model.dto;

import java.util.UUID;

public record GameResponse(UUID id, String name, String slug, String iconUrl) {
}
