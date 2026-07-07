package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByGameIdAndSlug(UUID gameId, String slug);

    Optional<Team> findByGameIdAndExternalId(UUID gameId, String externalId);
}
