package dev.mundorf.esportstracker.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FollowGamesRequest(@NotNull List<String> slugs) {
}
