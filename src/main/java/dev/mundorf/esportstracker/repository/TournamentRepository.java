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
     * <p>
     * Ordered by tier severity (INTERNATIONAL, then PRIMARY, then SECONDARY) via an explicit CASE
     * rather than a plain {@code ORDER BY t.tier} — {@code tier} is {@code @Enumerated(STRING)}, so a
     * raw column sort would be alphabetical, which only coincidentally matches severity order today
     * and would silently break if a tier name ever changed.
     */
    @EntityGraph(attributePaths = {"league", "game"})
    @Query("""
            SELECT t FROM Tournament t
            WHERE (:gameSlug IS NULL OR t.game.slug = :gameSlug)
              AND (:status IS NULL OR t.status = :status)
              AND (:tier IS NULL OR t.tier = :tier)
            ORDER BY CASE t.tier
                WHEN dev.mundorf.esportstracker.model.entity.TournamentTier.INTERNATIONAL THEN 0
                WHEN dev.mundorf.esportstracker.model.entity.TournamentTier.PRIMARY THEN 1
                ELSE 2
              END, t.startDate DESC
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
     * Tournaments active now or starting within the horizon window - same "in-season" scoping as
     * {@link #findLeaguesWithActiveTournaments}, but returning the tournaments themselves (with
     * their externalId) rather than just the parent league, since standings sync is per-tournament.
     */
    @Query("""
            SELECT t FROM Tournament t
            WHERE t.startDate <= :horizon AND t.endDate >= :today
            """)
    List<Tournament> findActiveTournaments(@Param("today") LocalDate today,
                                           @Param("horizon") LocalDate horizon);

    /**
     * Running tournaments for a user's followed leagues/teams (a followed team qualifies via any of
     * its currently scheduled matches in the tournament). Game-level follows deliberately don't feed
     * this query — see {@link MatchRepository#findUpcomingForFollowed}, which also documents the
     * empty-set caveat: callers must never pass an empty set.
     */
    @EntityGraph(attributePaths = {"league", "game"})
    @Query("""
            SELECT DISTINCT t FROM Tournament t
            WHERE t.status = dev.mundorf.esportstracker.model.entity.EventStatus.RUNNING
              AND (t.league.id IN :leagueIds
                   OR EXISTS (SELECT 1 FROM Match m
                              WHERE m.tournament = t AND (m.teamA.id IN :teamIds OR m.teamB.id IN :teamIds)))
            """)
    List<Tournament> findRunningForFollowed(@Param("leagueIds") Set<UUID> leagueIds,
                                            @Param("teamIds") Set<UUID> teamIds,
                                            Pageable pageable);
}
