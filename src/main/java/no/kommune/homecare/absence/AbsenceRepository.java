package no.kommune.homecare.absence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AbsenceRepository extends JpaRepository<Absence, UUID> {

    List<Absence> findByNurseId(UUID nurseId);

    List<Absence> findByNurseIdAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
            UUID nurseId, Instant end, Instant start);
}
