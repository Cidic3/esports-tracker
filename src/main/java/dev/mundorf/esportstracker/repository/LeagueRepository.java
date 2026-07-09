package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeagueRepository extends JpaRepository<League, UUID> {

    Optional<League> findByGameIdAndSlug(UUID gameId, String slug);

    Optional<League> findByGameIdAndExternalId(UUID gameId, String externalId);

    /** Settings-page league picker: one game's leagues, grouped by region client-side. */
    List<League> findByGameSlugOrderByRegionAscNameAsc(String gameSlug);

    List<League> findAllByOrderByRegionAscNameAsc();
}
