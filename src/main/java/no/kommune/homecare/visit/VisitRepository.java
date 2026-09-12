package no.kommune.homecare.visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {

    /**
     * Eagerly fetches {@code patient}/{@code nurse} on every method below so
     * {@code VisitResponse.from(...)} can read their names in the controller
     * after this repository's call returns - {@code spring.jpa.open-in-view}
     * is off, so the Hibernate session backing a lazy proxy is already closed
     * by then.
     */
    @Override
    @EntityGraph(attributePaths = {"patient", "nurse"})
    Optional<Visit> findById(UUID id);

    @EntityGraph(attributePaths = {"patient", "nurse"})
    Page<Visit> findByPatientId(UUID patientId, Pageable pageable);

    @EntityGraph(attributePaths = {"patient", "nurse"})
    Page<Visit> findByNurseId(UUID nurseId, Pageable pageable);

    List<Visit> findByNurseIdAndScheduledStartBetween(UUID nurseId, Instant from, Instant to);

    List<Visit> findByNurseIdAndStatusAndScheduledStartBetween(
            UUID nurseId, VisitStatus status, Instant from, Instant to);

    List<Visit> findByStatusAndScheduledStartBetween(VisitStatus status, Instant from, Instant to);

    @EntityGraph(attributePaths = {"patient", "nurse"})
    List<Visit> findByScheduledStartBetween(Instant from, Instant to);
}
