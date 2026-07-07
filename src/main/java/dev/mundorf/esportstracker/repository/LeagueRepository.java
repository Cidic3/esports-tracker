package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LeagueRepository extends JpaRepository<League, UUID> {

    Optional<League> findByGameIdAndSlug(UUID gameId, String slug);

    Optional<League> findByGameIdAndExternalId(UUID gameId, String externalId);
}
