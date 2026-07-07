package dev.mundorf.esportstracker.service;

import dev.mundorf.esportstracker.exception.ResourceNotFoundException;
import dev.mundorf.esportstracker.model.entity.EventStatus;
import dev.mundorf.esportstracker.model.entity.Tournament;
import dev.mundorf.esportstracker.model.entity.TournamentTier;
import dev.mundorf.esportstracker.repository.TournamentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    public Page<Tournament> search(String gameSlug, EventStatus status, TournamentTier tier, Pageable pageable) {
        return tournamentRepository.search(gameSlug, status, tier, pageable);
    }

    public Tournament findById(UUID id) {
        return tournamentRepository.findWithAssociationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
    }
}
