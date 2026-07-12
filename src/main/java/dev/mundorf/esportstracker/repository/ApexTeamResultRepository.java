package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.ApexTeamResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApexTeamResultRepository extends JpaRepository<ApexTeamResult, UUID> {

    Optional<ApexTeamResult> findByMatchDayIdAndTeamId(UUID matchDayId, UUID teamId);

    @EntityGraph(attributePaths = {"team"})
    List<ApexTeamResult> findByMatchDayIdOrderByRankAsc(UUID matchDayId);

    /** A team's most recent match day results, newest first - for the team detail page. */
    @EntityGraph(attributePaths = {"matchDay", "matchDay.tournament"})
    @Query("""
            SELECT r FROM ApexTeamResult r
            WHERE r.team.id = :teamId
            ORDER BY r.matchDay.startsAt DESC
            """)
    Page<ApexTeamResult> findRecentByTeamId(@Param("teamId") UUID teamId, Pageable pageable);
}
