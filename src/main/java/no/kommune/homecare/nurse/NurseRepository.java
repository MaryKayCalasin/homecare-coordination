package no.kommune.homecare.nurse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NurseRepository extends JpaRepository<Nurse, UUID> {

    Optional<Nurse> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    Page<Nurse> findByActiveTrue(Pageable pageable);

    List<Nurse> findByMunicipalityIgnoreCaseAndActiveTrue(String municipality);

    Optional<Nurse> findByUserId(UUID userId);
}
