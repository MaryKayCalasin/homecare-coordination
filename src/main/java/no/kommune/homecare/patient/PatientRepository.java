package no.kommune.homecare.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByNationalId(String nationalId);

    boolean existsByNationalId(String nationalId);

    Page<Patient> findByActiveTrue(Pageable pageable);

    Page<Patient> findByMunicipalityIgnoreCaseAndActiveTrue(String municipality, Pageable pageable);

    Page<Patient> findByFullNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("select distinct p.municipality from Patient p where p.active = true")
    List<String> findDistinctActiveMunicipalities();
}
