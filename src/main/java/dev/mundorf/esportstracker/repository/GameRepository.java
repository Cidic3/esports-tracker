package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    Optional<Game> findBySlug(String slug);
}
