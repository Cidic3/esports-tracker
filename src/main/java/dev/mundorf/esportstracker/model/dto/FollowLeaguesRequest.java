package dev.mundorf.esportstracker.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** version: the User.version the client last read - see StaleUpdateException. */
public record FollowLeaguesRequest(@NotNull List<UUID> leagueIds, @NotNull Long version) {
}
