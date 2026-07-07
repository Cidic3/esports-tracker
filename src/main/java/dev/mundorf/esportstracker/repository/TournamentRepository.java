package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.League;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

    Optional<Tournament> findByGameIdAndSlug(UUID gameId, String slug);

    Optional<Tournament> findByGameIdAndExternalId(UUID gameId, String externalId);

    @EntityGraph(attributePaths = {"league", "game"})
    Optional<Tournament> findWithAssociationsById(UUID id);

    /**
     * Filtered, paginated tournament list. Each filter is optional via the
     * "(:param IS NULL OR ...)" idiom. {@code @EntityGraph} eager-loads the associations the DTO
     * needs so mapping after the session closes is safe and doesn't trigger N+1 queries.
     */
    @EntityGraph(attributePaths = {"league", "game"})
    @Query("""
            SELECT t FROM Tournament t
            WHERE (:gameSlug IS NULL OR t.game.slug = :gameSlug)
              AND (:status IS NULL OR t.status = :status)
              AND (:tier IS NULL OR t.tier = :tier)
            """)
    Page<Tournament> search(@Param("gameSlug") String gameSlug,
                            @Param("status") EventStatus status,
                            @Param("tier") TournamentTier tier,
                            Pageable pageable);

    /**
     * Leagues that have a tournament active now or starting within the horizon window
     * (startDate <= horizon AND endDate >= today). Used to scope the frequent match-sync poll to
     * "in-season" leagues only, instead of hitting the schedule endpoint for all ~20 leagues.
     */
    @Query("""
            SELECT DISTINCT t.league FROM Tournament t
            WHERE t.startDate <= :horizon AND t.endDate >= :today
            """)
    List<League> findLeaguesWithActiveTournaments(@Param("today") LocalDate today,
                                                  @Param("horizon") LocalDate horizon);
}
