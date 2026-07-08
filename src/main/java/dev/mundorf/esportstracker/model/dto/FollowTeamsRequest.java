package dev.mundorf.esportstracker.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record FollowTeamsRequest(@NotNull List<UUID> teamIds) {
}
