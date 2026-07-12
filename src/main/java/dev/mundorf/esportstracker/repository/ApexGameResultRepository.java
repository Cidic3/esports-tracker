package dev.mundorf.esportstracker.repository;

import dev.mundorf.esportstracker.model.entity.ApexGameResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ApexGameResultRepository extends JpaRepository<ApexGameResult, UUID> {

    List<ApexGameResult> findByTeamResultId(UUID teamResultId);

    /** All per-game rows for a match day's team results in one query (avoids N+1 in the detail view). */
    List<ApexGameResult> findByTeamResultIdIn(Collection<UUID> teamResultIds);
}
