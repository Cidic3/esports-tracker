package dev.mundorf.esportstracker.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** version: the User.version the client last read - see StaleUpdateException. */
public record FollowGamesRequest(@NotNull List<String> slugs, @NotNull Long version) {
}
