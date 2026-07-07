package dev.mundorf.esportstracker.client.riot.dto;

import java.util.List;

public record RiotMatch(String id, List<RiotMatchTeam> teams) {
}
