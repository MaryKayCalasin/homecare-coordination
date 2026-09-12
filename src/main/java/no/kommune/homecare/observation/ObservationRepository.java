package no.kommune.homecare.observation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ObservationRepository extends JpaRepository<Observation, UUID> {

    Page<Observation> findByPatientIdOrderByRecordedAtDesc(UUID patientId, Pageable pageable);

    List<Observation> findByUrgentTrueAndUrgentResolvedFalseOrderByRecordedAtDesc();

    List<Observation> findByRecordedAtBetweenOrderByRecordedAtAsc(Instant from, Instant to);

    List<Observation> findByUrgentTrueAndRecordedAtBetween(Instant from, Instant to);
}
