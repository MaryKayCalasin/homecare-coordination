package no.kommune.homecare.visit;

import lombok.RequiredArgsConstructor;
import no.kommune.homecare.audit.AuditAction;
import no.kommune.homecare.audit.Auditable;
import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.common.exception.ResourceNotFoundException;
import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.nurse.NurseService;
import no.kommune.homecare.patient.Patient;
import no.kommune.homecare.patient.PatientService;
import no.kommune.homecare.security.CurrentUser;
import no.kommune.homecare.vedtak.Vedtak;
import no.kommune.homecare.vedtak.VedtakService;
import no.kommune.homecare.visit.dto.VisitRequest;
import no.kommune.homecare.websocket.WebSocketEventPublisher;
import no.kommune.homecare.websocket.dto.VisitUpdateMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitService {

    private final VisitRepository visitRepository;
    private final PatientService patientService;
    private final NurseService nurseService;
    private final WebSocketEventPublisher eventPublisher;
    private final VedtakService vedtakService;

    @Auditable(action = AuditAction.CREATE, entityType = "Visit", details = "Visit scheduled")
    @Transactional
    public Visit create(VisitRequest request) {
        if (!request.scheduledStart().isBefore(request.scheduledEnd())) {
            throw new BusinessRuleException("Visit start must be before end");
        }
        Patient patient = patientService.getById(request.patientId());
        Nurse nurse = request.nurseId() == null ? null : nurseService.getById(request.nurseId());

        if (nurse != null) {
            // patientService.getById/nurseService.getById already enforce that
            // both belong to the caller's own municipality when scoped.
            assertNoConflict(nurse.getId(), patient, request.scheduledStart(), request.scheduledEnd(), null);
            assertQualified(nurse, request.visitType());
        }

        Visit visit = Visit.builder()
                .patient(patient)
                .nurse(nurse)
                .scheduledStart(request.scheduledStart())
                .scheduledEnd(request.scheduledEnd())
                .status(VisitStatus.SCHEDULED)
                .visitType(request.visitType())
                .notes(request.notes())
                .location(request.location() != null ? request.location() : patient.getAddress())
                .build();

        Visit saved = visitRepository.save(visit);
        publish(saved, "CREATED", "Visit scheduled");
        return saved;
    }

    public Visit getById(UUID id) {
        Visit visit = visitRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Visit", id));
        CurrentUser.assertAccessible(visit.getPatient().getMunicipality());
        return visit;
    }

    public Page<Visit> findByPatient(UUID patientId, Pageable pageable) {
        // patientService is intentionally not called here to avoid an extra
        // lookup on every page; scoping instead filters the returned visits,
        // consistent with the other list methods below.
        return filterToOwnMunicipality(visitRepository.findByPatientId(patientId, pageable));
    }

    public Page<Visit> findByNurse(UUID nurseId, Pageable pageable) {
        return filterToOwnMunicipality(visitRepository.findByNurseId(nurseId, pageable));
    }

    public List<Visit> findByNurseAndRange(UUID nurseId, Instant from, Instant to) {
        return filterToOwnMunicipality(visitRepository.findByNurseIdAndScheduledStartBetween(nurseId, from, to));
    }

    public List<Visit> findByStatusAndRange(VisitStatus status, Instant from, Instant to) {
        return filterToOwnMunicipality(visitRepository.findByStatusAndScheduledStartBetween(status, from, to));
    }

    public List<Visit> findByRange(Instant from, Instant to) {
        return filterToOwnMunicipality(visitRepository.findByScheduledStartBetween(from, to));
    }

    /**
     * Drops visits belonging to another kommune for a municipality-scoped
     * caller; a platform-wide account (or no authenticated caller at all,
     * e.g. the redistribution engine acting internally) sees everything.
     * Filtered in application code rather than as a repository predicate
     * since several of these queries don't join {@code patient} otherwise.
     */
    private List<Visit> filterToOwnMunicipality(List<Visit> visits) {
        return CurrentUser.municipality()
                .map(m -> visits.stream().filter(v -> m.equalsIgnoreCase(v.getPatient().getMunicipality())).toList())
                .orElse(visits);
    }

    private Page<Visit> filterToOwnMunicipality(Page<Visit> visits) {
        return CurrentUser.municipality()
                .<Page<Visit>>map(m -> new PageImpl<>(
                        visits.getContent().stream().filter(v -> m.equalsIgnoreCase(v.getPatient().getMunicipality())).toList(),
                        visits.getPageable(), visits.getTotalElements()))
                .orElse(visits);
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Visit", details = "Visit rescheduled")
    @Transactional
    public Visit reschedule(UUID id, Instant newStart, Instant newEnd) {
        Visit visit = getById(id);
        if (!newStart.isBefore(newEnd)) {
            throw new BusinessRuleException("Visit start must be before end");
        }
        if (visit.getNurse() != null) {
            assertNoConflict(visit.getNurse().getId(), visit.getPatient(), newStart, newEnd, visit.getId());
        }
        visit.setScheduledStart(newStart);
        visit.setScheduledEnd(newEnd);
        publish(visit, "RESCHEDULED", "Visit rescheduled");
        return visit;
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Visit", details = "Visit reassigned to a different nurse")
    @Transactional
    public Visit assignNurse(UUID visitId, UUID nurseId) {
        Visit visit = getById(visitId);
        Nurse nurse = nurseService.getById(nurseId);
        assertNoConflict(nurse.getId(), visit.getPatient(), visit.getScheduledStart(), visit.getScheduledEnd(), visit.getId());
        assertQualified(nurse, visit.getVisitType());
        visit.setNurse(nurse);
        if (visit.getStatus() == VisitStatus.CANCELLED || visit.getStatus() == VisitStatus.MISSED) {
            visit.setStatus(VisitStatus.SCHEDULED);
        }
        publish(visit, "REASSIGNED", "Visit reassigned to " + nurse.getFullName());
        return visit;
    }

    @Transactional
    public Visit start(UUID id) {
        Visit visit = getById(id);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setActualStart(Instant.now());
        publish(visit, "STARTED", "Visit started");
        return visit;
    }

    @Transactional
    public Visit complete(UUID id, String completionNotes) {
        Visit visit = getById(id);
        visit.setStatus(VisitStatus.COMPLETED);
        visit.setActualEnd(Instant.now());
        if (completionNotes != null && !completionNotes.isBlank()) {
            visit.setNotes(visit.getNotes() == null ? completionNotes : visit.getNotes() + "\n" + completionNotes);
        }
        publish(visit, "COMPLETED", "Visit completed");
        return visit;
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Visit", details = "Visit cancelled")
    @Transactional
    public Visit cancel(UUID id, String reason) {
        Visit visit = getById(id);
        visit.setStatus(VisitStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            visit.setNotes(visit.getNotes() == null ? reason : visit.getNotes() + "\n" + reason);
        }
        publish(visit, "CANCELLED", "Visit cancelled");
        return visit;
    }

    @Transactional
    public Visit markMissed(UUID id) {
        Visit visit = getById(id);
        visit.setStatus(VisitStatus.MISSED);
        publish(visit, "MISSED", "Visit marked as missed");
        return visit;
    }

    /**
     * Detaches a visit from its nurse without deleting it, used when a nurse
     * becomes absent and no substitute could be found automatically.
     */
    @Transactional
    public Visit unassign(UUID id) {
        Visit visit = getById(id);
        visit.setNurse(null);
        publish(visit, "UNASSIGNED", "Visit unassigned, awaiting a new nurse");
        return visit;
    }

    private void assertNoConflict(UUID nurseId, Patient patient, Instant start, Instant end, UUID excludeVisitId) {
        List<Visit> nearby = visitRepository.findByNurseIdAndScheduledStartBetween(
                        nurseId, start.minusSeconds(24 * 3600), end.plusSeconds(24 * 3600)).stream()
                .filter(v -> !v.getId().equals(excludeVisitId))
                .filter(v -> v.getStatus() != VisitStatus.CANCELLED)
                .toList();

        if (nearby.stream().anyMatch(v -> v.overlaps(start, end))) {
            throw new BusinessRuleException("Nurse already has a conflicting visit in this time window");
        }
        if (!TravelTime.isFeasible(nearby, patient, start, end)) {
            throw new BusinessRuleException(
                    "Not enough travel time for the nurse to reach this patient from an adjacent visit");
        }
    }

    /**
     * Records which vedtak (statutory decision) entitles the patient to this
     * visit. Purely a traceability link today - see {@link Vedtak}'s class
     * Javadoc for what enforcing granted hours would additionally require.
     */
    @Auditable(action = AuditAction.UPDATE, entityType = "Visit", details = "Visit linked to a vedtak")
    @Transactional
    public Visit linkVedtak(UUID visitId, UUID vedtakId) {
        Visit visit = getById(visitId);
        Vedtak vedtak = vedtakService.getById(vedtakId);
        if (!vedtak.getPatient().getId().equals(visit.getPatient().getId())) {
            throw new BusinessRuleException("This vedtak belongs to a different patient than the visit");
        }
        visit.setVedtak(vedtak);
        return visit;
    }

    private void assertQualified(Nurse nurse, VisitType visitType) {
        if (!VisitCapability.isQualified(nurse, visitType)) {
            throw new BusinessRuleException(
                    "Nurse " + nurse.getFullName() + " is not qualified for a " + visitType + " visit");
        }
    }

    private void publish(Visit visit, String eventType, String message) {
        eventPublisher.publishVisitUpdate(VisitUpdateMessage.of(
                visit.getId(),
                visit.getPatient().getId(),
                visit.getNurse() == null ? null : visit.getNurse().getId(),
                visit.getStatus().name(),
                eventType,
                message
        ));
    }
}
