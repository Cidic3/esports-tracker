package dev.mundorf.esportstracker.model.dto;

import java.util.UUID;

public record TeamResponse(UUID id, String name, String slug, String logoUrl) {
}
