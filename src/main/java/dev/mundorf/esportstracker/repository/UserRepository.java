package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    // Multiple eager collections are safe here because they're Sets — Hibernate's
    // MultipleBagFetchException only applies to Lists (bags).
    // followedTeams.game/league are included because the feed resolves followed Apex teams to
    // their home league AFTER this query's session has closed (open-in-view is off) — without
    // them, ApexMatchDayService.findUpcomingForUser dies on a LazyInitializationException.
    @EntityGraph(attributePaths = {
            "followedGames", "followedTeams", "followedTeams.game", "followedTeams.league", "followedLeagues"})
    Optional<User> findWithFollowsByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
