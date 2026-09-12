package no.kommune.homecare.handover;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.kommune.homecare.common.exception.ResourceNotFoundException;
import no.kommune.homecare.observation.Observation;
import no.kommune.homecare.observation.ObservationService;
import no.kommune.homecare.patient.PatientService;
import no.kommune.homecare.visit.Visit;
import no.kommune.homecare.visit.VisitService;
import no.kommune.homecare.visit.VisitStatus;
import no.kommune.homecare.websocket.WebSocketEventPublisher;
import no.kommune.homecare.websocket.dto.HandoverReportMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Generates the report that hands one shift's knowledge of each patient off
 * to the next: what care was delivered, what was missed, and which urgent
 * flags are still open. Runs automatically at every shift boundary and can
 * also be triggered on demand by a coordinator.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private static final ZoneId OSLO = ZoneId.of("Europe/Oslo");

    private final ShiftHandoverReportRepository reportRepository;
    private final VisitService visitService;
    private final ObservationService observationService;
    private final PatientService patientService;
    private final WebSocketEventPublisher eventPublisher;

    /**
     * Fires at the start of each shift (07:00, 15:00, 23:00 Oslo time), which
     * is exactly when the previous shift ends, and generates its handover
     * report for every municipality that currently has active patients.
     */
    @Scheduled(cron = "0 0 7,15,23 * * *", zone = "Europe/Oslo")
    public void generateForEndedShiftAcrossMunicipalities() {
        ZonedDateTime now = ZonedDateTime.now(OSLO);
        ShiftType endedShift = shiftEndingAt(now.toLocalTime());
        LocalDate shiftDate = endedShift == ShiftType.NIGHT ? now.toLocalDate().minusDays(1) : now.toLocalDate();

        for (String municipality : patientService.findActiveMunicipalities()) {
            try {
                generate(shiftDate, endedShift, municipality, "system");
            } catch (Exception ex) {
                log.error("Failed to auto-generate {} handover report for {}", endedShift, municipality, ex);
            }
        }
    }

    private ShiftType shiftEndingAt(LocalTime time) {
        if (time.equals(LocalTime.of(7, 0))) {
            return ShiftType.NIGHT;
        }
        if (time.equals(LocalTime.of(15, 0))) {
            return ShiftType.DAY;
        }
        return ShiftType.EVENING;
    }

    @Transactional
    public ShiftHandoverReport generate(LocalDate shiftDate, ShiftType shiftType, String municipality, String generatedBy) {
        Instant start = shiftStart(shiftDate, shiftType);
        Instant end = shiftEnd(shiftDate, shiftType);

        List<Visit> visits = visitService.findByRange(start, end).stream()
                .filter(v -> municipality.equalsIgnoreCase(v.getPatient().getMunicipality()))
                .toList();

        List<Observation> observations = observationService.findByRange(start, end).stream()
                .filter(o -> municipality.equalsIgnoreCase(o.getPatient().getMunicipality()))
                .toList();

        int completed = (int) visits.stream().filter(v -> v.getStatus() == VisitStatus.COMPLETED).count();
        int missed = (int) visits.stream().filter(v -> v.getStatus() == VisitStatus.MISSED).count();
        int cancelled = (int) visits.stream().filter(v -> v.getStatus() == VisitStatus.CANCELLED).count();
        int pending = (int) visits.stream()
                .filter(v -> v.getStatus() == VisitStatus.SCHEDULED || v.getStatus() == VisitStatus.IN_PROGRESS)
                .count();

        List<Observation> urgent = observations.stream().filter(Observation::isUrgent).toList();
        List<Observation> unresolvedUrgent = urgent.stream().filter(o -> !o.isUrgentResolved()).toList();

        String summary = buildSummary(shiftDate, shiftType, municipality, visits, observations, urgent, unresolvedUrgent,
                completed, missed, cancelled, pending);

        ShiftHandoverReport report = ShiftHandoverReport.builder()
                .shiftDate(shiftDate)
                .shiftType(shiftType)
                .municipality(municipality)
                .generatedBy(generatedBy != null ? generatedBy : currentUsername())
                .visitsCompletedCount(completed)
                .visitsMissedCount(missed)
                .visitsCancelledCount(cancelled)
                .visitsPendingCount(pending)
                .observationsCount(observations.size())
                .urgentObservationsCount(urgent.size())
                .unresolvedUrgentCount(unresolvedUrgent.size())
                .summary(summary)
                .build();

        ShiftHandoverReport saved = reportRepository.save(report);

        eventPublisher.publishHandoverReport(new HandoverReportMessage(
                saved.getId(), saved.getShiftDate(), saved.getShiftType().name(),
                saved.getUnresolvedUrgentCount(), Instant.now()));

        return saved;
    }

    public ShiftHandoverReport generate(LocalDate shiftDate, ShiftType shiftType, String municipality) {
        return generate(shiftDate, shiftType, municipality, currentUsername());
    }

    public ShiftHandoverReport getById(UUID id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ShiftHandoverReport", id));
    }

    public List<ShiftHandoverReport> findByMunicipality(String municipality) {
        return reportRepository.findByMunicipalityIgnoreCaseOrderByShiftDateDesc(municipality);
    }

    public Page<ShiftHandoverReport> findAll(Pageable pageable) {
        return reportRepository.findAllByOrderByShiftDateDesc(pageable);
    }

    private String buildSummary(LocalDate shiftDate, ShiftType shiftType, String municipality,
                                 List<Visit> visits, List<Observation> observations,
                                 List<Observation> urgent, List<Observation> unresolvedUrgent,
                                 int completed, int missed, int cancelled, int pending) {
        StringBuilder sb = new StringBuilder();
        sb.append("Shift handover report - ").append(shiftType).append(" shift, ")
                .append(shiftDate).append(", ").append(municipality).append("\n\n");

        sb.append("Visits: ").append(visits.size()).append(" total - ")
                .append(completed).append(" completed, ")
                .append(missed).append(" missed, ")
                .append(cancelled).append(" cancelled, ")
                .append(pending).append(" still pending\n");

        sb.append("Observations recorded: ").append(observations.size())
                .append(" (").append(urgent.size()).append(" urgent, ")
                .append(unresolvedUrgent.size()).append(" still unresolved)\n\n");

        if (!unresolvedUrgent.isEmpty()) {
            sb.append("UNRESOLVED URGENT FLAGS - requires immediate attention from the incoming shift:\n");
            for (Observation o : unresolvedUrgent) {
                sb.append("  - ").append(o.getPatient().getFullName())
                        .append(": ").append(o.getDescription())
                        .append(" (recorded by ").append(o.getRecordedBy().getFullName())
                        .append(" at ").append(o.getRecordedAt()).append(")\n");
            }
            sb.append("\n");
        }

        if (missed > 0) {
            sb.append("Missed visits:\n");
            visits.stream()
                    .filter(v -> v.getStatus() == VisitStatus.MISSED)
                    .forEach(v -> sb.append("  - ").append(v.getPatient().getFullName())
                            .append(" at ").append(v.getScheduledStart()).append("\n"));
            sb.append("\n");
        }

        long medicationsGiven = observations.stream()
                .filter(o -> Boolean.TRUE.equals(o.getMedicationGiven()))
                .count();
        sb.append("Medications administered during shift: ").append(medicationsGiven).append("\n");

        return sb.toString();
    }

    private Instant shiftStart(LocalDate date, ShiftType shiftType) {
        return LocalDateTime.of(date, shiftType.defaultStart()).atZone(OSLO).toInstant();
    }

    private Instant shiftEnd(LocalDate date, ShiftType shiftType) {
        LocalDate endDate = shiftType.crossesMidnight() ? date.plusDays(1) : date;
        return LocalDateTime.of(endDate, shiftType.defaultEnd()).atZone(OSLO).toInstant();
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
