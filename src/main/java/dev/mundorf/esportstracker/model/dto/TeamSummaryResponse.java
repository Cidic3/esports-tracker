package dev.mundorf.esportstracker.model.dto;

import java.util.UUID;

/**
 * Team browser list-item shape - unlike {@link TeamResponse} (embedded in Match/Standing, where the
 * game is already contextual), a global team search can span games, so the game is included here.
 */
public record TeamSummaryResponse(UUID id, String name, String slug, String logoUrl, String gameSlug) {
}
