package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"followedGames", "followedTeams"})
    Optional<User> findWithFollowsByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
