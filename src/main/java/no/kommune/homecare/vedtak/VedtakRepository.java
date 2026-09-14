package no.kommune.homecare.vedtak;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VedtakRepository extends JpaRepository<Vedtak, UUID> {

    @Override
    @EntityGraph(attributePaths = "patient")
    Optional<Vedtak> findById(UUID id);

    @EntityGraph(attributePaths = "patient")
    List<Vedtak> findByPatientIdOrderByValidFromDesc(UUID patientId);
}
