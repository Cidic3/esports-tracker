package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.Standing;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StandingRepository extends JpaRepository<Standing, UUID> {

    Optional<Standing> findByTournamentIdAndTeamIdAndGroupName(UUID tournamentId, UUID teamId, String groupName);

    @EntityGraph(attributePaths = {"team", "tournament"})
    List<Standing> findByTournamentIdOrderByGroupNameAscRankAsc(UUID tournamentId);

    @EntityGraph(attributePaths = {"tournament", "team"})
    List<Standing> findByTeamId(UUID teamId);
}
