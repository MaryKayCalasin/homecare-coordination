package no.kommune.homecare.visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {

    Page<Visit> findByPatientId(UUID patientId, Pageable pageable);

    Page<Visit> findByNurseId(UUID nurseId, Pageable pageable);

    List<Visit> findByNurseIdAndScheduledStartBetween(UUID nurseId, Instant from, Instant to);

    List<Visit> findByNurseIdAndStatusAndScheduledStartBetween(
            UUID nurseId, VisitStatus status, Instant from, Instant to);

    List<Visit> findByStatusAndScheduledStartBetween(VisitStatus status, Instant from, Instant to);

    List<Visit> findByScheduledStartBetween(Instant from, Instant to);
}
