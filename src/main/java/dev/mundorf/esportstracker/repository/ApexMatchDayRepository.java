package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.ApexMatchDay;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ApexMatchDayRepository extends JpaRepository<ApexMatchDay, UUID> {

    Optional<ApexMatchDay> findByGameIdAndExternalId(UUID gameId, String externalId);

    @EntityGraph(attributePaths = {"tournament", "tournament.league", "game"})
    Optional<ApexMatchDay> findWithAssociationsById(UUID id);

    /** Filtered, paginated match day list - same optional-filter idiom as MatchRepository.search. */
    @EntityGraph(attributePaths = {"tournament", "tournament.league", "game"})
    @Query("""
            SELECT d FROM ApexMatchDay d
            WHERE (:leagueSlug IS NULL OR d.tournament.league.slug = :leagueSlug)
              AND (:status IS NULL OR d.status = :status)
            """)
    Page<ApexMatchDay> search(@Param("leagueSlug") String leagueSlug,
                              @Param("status") EventStatus status,
                              Pageable pageable);

    /**
     * Upcoming match days for a set of followed leagues. Unlike MatchRepository's followed
     * queries there is no team-side OR here: a pending BR match day has no team list yet (teams
     * only appear once results land), so the service resolves followed Apex teams to their home
     * league before calling this - see ApexMatchDayService.
     */
    @EntityGraph(attributePaths = {"tournament", "tournament.league", "game"})
    @Query("""
            SELECT d FROM ApexMatchDay d
            WHERE d.status = dev.mundorf.esportstracker.model.entity.EventStatus.UPCOMING
              AND d.tournament.league.id IN :leagueIds
            """)
    Page<ApexMatchDay> findUpcomingForLeagues(@Param("leagueIds") Set<UUID> leagueIds, Pageable pageable);
}
