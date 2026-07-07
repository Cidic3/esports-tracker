package dev.mundorf.esportstracker.client.riot.dto;

import java.time.Instant;

public record RiotScheduleEvent(
        Instant startTime,
        String state,
        String type,
        String blockName,
        RiotEventLeague league,
        RiotMatch match
) {
}
