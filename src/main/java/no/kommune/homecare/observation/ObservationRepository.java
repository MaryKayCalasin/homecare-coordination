package no.kommune.homecare.observation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObservationRepository extends JpaRepository<Observation, UUID> {

    /**
     * Eagerly fetches {@code patient}/{@code recordedBy} on every method below
     * so {@code ObservationResponse.from(...)} can read their names in the
     * controller after this repository's call returns - {@code
     * spring.jpa.open-in-view} is off, so the Hibernate session backing a
     * lazy proxy is already closed by then.
     */
    @Override
    @EntityGraph(attributePaths = {"patient", "recordedBy"})
    Optional<Observation> findById(UUID id);

    @EntityGraph(attributePaths = {"patient", "recordedBy"})
    Page<Observation> findByPatientIdOrderByRecordedAtDesc(UUID patientId, Pageable pageable);

    @EntityGraph(attributePaths = {"patient", "recordedBy"})
    List<Observation> findByUrgentTrueAndUrgentResolvedFalseOrderByRecordedAtDesc();

    @EntityGraph(attributePaths = "patient")
    List<Observation> findByRecordedAtBetweenOrderByRecordedAtAsc(Instant from, Instant to);

    @EntityGraph(attributePaths = "patient")
    List<Observation> findByUrgentTrueAndRecordedAtBetween(Instant from, Instant to);
}
