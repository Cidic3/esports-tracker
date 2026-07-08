package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    Optional<Match> findByGameIdAndExternalId(UUID gameId, String externalId);

    @EntityGraph(attributePaths = {"teamA", "teamB", "tournament", "game"})
    Optional<Match> findWithAssociationsById(UUID id);

    @EntityGraph(attributePaths = {"teamA", "teamB", "tournament", "game"})
    Page<Match> findByTournamentId(UUID tournamentId, Pageable pageable);

    @EntityGraph(attributePaths = {"teamA", "teamB", "tournament", "game"})
    List<Match> findByScheduledAtBetweenOrderByScheduledAtAsc(Instant from, Instant to);

    /**
     * Filtered, paginated match list. Optional filters via the "(:param IS NULL OR ...)" idiom;
     * the team filter matches either side of the match. {@code @EntityGraph} eager-loads
     * associations to avoid N+1 and lazy-init errors during mapping.
     */
    // The cast(...) on the nullable UUID/timestamp params is required for PostgreSQL: a param that
    // only appears in an "IS NULL" check has no inferable type otherwise ("could not determine data
    // type of parameter"). String/enum params don't need it (Postgres defaults them to text).
    @EntityGraph(attributePaths = {"teamA", "teamB", "tournament", "game"})
    @Query("""
            SELECT m FROM Match m
            WHERE (:gameSlug IS NULL OR m.game.slug = :gameSlug)
              AND (:status IS NULL OR m.status = :status)
              AND (cast(:teamId as java.util.UUID) IS NULL OR m.teamA.id = :teamId OR m.teamB.id = :teamId)
              AND (cast(:from as java.time.Instant) IS NULL OR m.scheduledAt >= :from)
              AND (cast(:to as java.time.Instant) IS NULL OR m.scheduledAt <= :to)
            """)
    Page<Match> search(@Param("gameSlug") String gameSlug,
                       @Param("status") EventStatus status,
                       @Param("teamId") UUID teamId,
                       @Param("from") Instant from,
                       @Param("to") Instant to,
                       Pageable pageable);

    /**
     * Upcoming matches for a user's followed games/teams (OR semantics: qualifies if the match's game
     * is followed, or either side is a followed team). Callers must never pass an empty set for either
     * parameter — an empty "IN ()" list is not portable across JPA providers — so the service layer
     * substitutes a random non-matching UUID when a user follows nothing of that kind.
     */
    @EntityGraph(attributePaths = {"teamA", "teamB", "tournament", "game"})
    @Query("""
            SELECT m FROM Match m
            WHERE m.status = dev.mundorf.esportstracker.model.entity.EventStatus.UPCOMING
              AND (m.game.id IN :gameIds OR m.teamA.id IN :teamIds OR m.teamB.id IN :teamIds)
            """)
    Page<Match> findUpcomingForFollowed(@Param("gameIds") Set<UUID> gameIds,
                                        @Param("teamIds") Set<UUID> teamIds,
                                        Pageable pageable);
}
