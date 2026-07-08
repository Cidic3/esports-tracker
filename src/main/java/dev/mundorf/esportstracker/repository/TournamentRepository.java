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
import java.util.Set;
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

    /**
     * Running tournaments for a user's followed games/teams (a followed team qualifies via any of its
     * currently scheduled matches in the tournament). Same empty-set caveat as
     * {@link MatchRepository#findUpcomingForFollowed}: callers must never pass an empty set.
     */
    @EntityGraph(attributePaths = {"league", "game"})
    @Query("""
            SELECT DISTINCT t FROM Tournament t
            WHERE t.status = dev.mundorf.esportstracker.model.entity.EventStatus.RUNNING
              AND (t.game.id IN :gameIds
                   OR EXISTS (SELECT 1 FROM Match m
                              WHERE m.tournament = t AND (m.teamA.id IN :teamIds OR m.teamB.id IN :teamIds)))
            """)
    List<Tournament> findRunningForFollowed(@Param("gameIds") Set<UUID> gameIds,
                                            @Param("teamIds") Set<UUID> teamIds,
                                            Pageable pageable);
}
