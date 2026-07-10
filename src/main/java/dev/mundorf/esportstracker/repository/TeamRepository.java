package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByGameIdAndSlug(UUID gameId, String slug);

    Optional<Team> findByGameIdAndExternalId(UUID gameId, String externalId);

    @EntityGraph(attributePaths = {"game", "organization", "league"})
    Optional<Team> findWithAssociationsById(UUID id);

    /**
     * Team browser search: optional game filter, optional case-insensitive name-contains search.
     * Same "(:param IS NULL OR ...)" idiom as {@code TournamentRepository.search}/{@code
     * MatchRepository.search}.
     */
    // Every occurrence of :search needs its own cast(:search as string): each JPQL occurrence of a
    // named parameter becomes a separate "?" placeholder in the generated SQL, bound and typed
    // independently by the JDBC driver - a null with no type hint defaults to bytea ("function
    // lower(bytea) does not exist"), and casting only the IS NULL occurrence (MatchRepository's
    // pattern for teamId/from/to) doesn't help the other one buried inside CONCAT.
    @EntityGraph(attributePaths = {"game", "organization"})
    @Query("""
            SELECT t FROM Team t
            WHERE (:gameSlug IS NULL OR t.game.slug = :gameSlug)
              AND (cast(:search as string) IS NULL
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))
            """)
    Page<Team> search(@Param("gameSlug") String gameSlug, @Param("search") String search, Pageable pageable);
}
