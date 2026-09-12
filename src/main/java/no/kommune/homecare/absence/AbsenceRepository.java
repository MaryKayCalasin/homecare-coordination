package no.kommune.homecare.absence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AbsenceRepository extends JpaRepository<Absence, UUID> {

    /**
     * Eagerly fetches {@code nurse} on every method below so {@code
     * AbsenceResponse.from(...)} can read its name in the controller after
     * this repository's call returns - {@code spring.jpa.open-in-view} is
     * off, so the Hibernate session backing a lazy proxy is already closed
     * by then.
     */
    @Override
    @EntityGraph(attributePaths = "nurse")
    Optional<Absence> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "nurse")
    List<Absence> findAll();

    @EntityGraph(attributePaths = "nurse")
    List<Absence> findByNurseId(UUID nurseId);

    List<Absence> findByNurseIdAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
            UUID nurseId, Instant end, Instant start);
}
