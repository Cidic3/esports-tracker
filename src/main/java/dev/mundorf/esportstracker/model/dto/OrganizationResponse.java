package dev.mundorf.esportstracker.model.dto;

import java.util.UUID;

public record OrganizationResponse(UUID id, String name, String slug, String logoUrl) {
}
