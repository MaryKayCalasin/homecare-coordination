package no.kommune.homecare.handover;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftHandoverReportRepository extends JpaRepository<ShiftHandoverReport, UUID> {

    List<ShiftHandoverReport> findByMunicipalityIgnoreCaseOrderByShiftDateDesc(String municipality);

    Optional<ShiftHandoverReport> findByMunicipalityIgnoreCaseAndShiftDateAndShiftType(
            String municipality, LocalDate shiftDate, ShiftType shiftType);

    Page<ShiftHandoverReport> findAllByOrderByShiftDateDesc(Pageable pageable);
}
