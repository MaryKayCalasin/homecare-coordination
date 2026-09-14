package no.kommune.homecare.observation;

import lombok.RequiredArgsConstructor;
import no.kommune.homecare.audit.AuditAction;
import no.kommune.homecare.audit.Auditable;
import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.common.exception.ResourceNotFoundException;
import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.nurse.NurseService;
import no.kommune.homecare.observation.dto.ObservationRequest;
import no.kommune.homecare.patient.Patient;
import no.kommune.homecare.patient.PatientService;
import no.kommune.homecare.security.CurrentUser;
import no.kommune.homecare.visit.Visit;
import no.kommune.homecare.visit.VisitService;
import no.kommune.homecare.websocket.WebSocketEventPublisher;
import no.kommune.homecare.websocket.dto.UrgentAlertMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObservationService {

    private final ObservationRepository observationRepository;
    private final PatientService patientService;
    private final NurseService nurseService;
    private final VisitService visitService;
    private final WebSocketEventPublisher eventPublisher;

    @Auditable(action = AuditAction.CREATE, entityType = "Observation", details = "Patient observation recorded")
    @Transactional
    public Observation create(ObservationRequest request) {
        Patient patient = patientService.getById(request.patientId());
        Nurse nurse = nurseService.getById(request.recordedByNurseId());
        Visit visit = request.visitId() == null ? null : visitService.getById(request.visitId());

        if (request.observationType() == ObservationType.MEDICATION
                && (request.medicationName() == null || request.medicationName().isBlank())) {
            throw new BusinessRuleException("Medication name is required for medication observations");
        }

        Observation observation = Observation.builder()
                .patient(patient)
                .recordedBy(nurse)
                .visit(visit)
                .observationType(request.observationType())
                .medicationName(request.medicationName())
                .medicationDosage(request.medicationDosage())
                .medicationGiven(request.medicationGiven())
                .moodLevel(request.moodLevel())
                .description(request.description())
                .urgent(request.urgent())
                .urgentResolved(false)
                .recordedAt(Instant.now())
                .build();

        Observation saved = observationRepository.save(observation);

        if (saved.isUrgent()) {
            eventPublisher.publishUrgentAlert(new UrgentAlertMessage(
                    saved.getId(), patient.getId(), patient.getFullName(),
                    nurse.getId(), nurse.getFullName(), saved.getDescription(), Instant.now()));
        }

        return saved;
    }

    public Observation getById(UUID id) {
        Observation observation = observationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Observation", id));
        CurrentUser.assertAccessible(observation.getPatient().getMunicipality());
        return observation;
    }

    @Auditable(action = AuditAction.READ, entityType = "Observation", details = "Patient journal accessed")
    public Page<Observation> findByPatient(UUID patientId, Pageable pageable) {
        // Throws if the patient belongs to another kommune than the caller.
        patientService.getById(patientId);
        return observationRepository.findByPatientIdOrderByRecordedAtDesc(patientId, pageable);
    }

    public List<Observation> findUnresolvedUrgent() {
        return filterToOwnMunicipality(observationRepository.findByUrgentTrueAndUrgentResolvedFalseOrderByRecordedAtDesc());
    }

    public List<Observation> findByRange(Instant from, Instant to) {
        return filterToOwnMunicipality(observationRepository.findByRecordedAtBetweenOrderByRecordedAtAsc(from, to));
    }

    public List<Observation> findUrgentByRange(Instant from, Instant to) {
        return filterToOwnMunicipality(observationRepository.findByUrgentTrueAndRecordedAtBetween(from, to));
    }

    private List<Observation> filterToOwnMunicipality(List<Observation> observations) {
        return CurrentUser.municipality()
                .map(m -> observations.stream()
                        .filter(o -> m.equalsIgnoreCase(o.getPatient().getMunicipality()))
                        .toList())
                .orElse(observations);
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Observation", details = "Urgent flag resolved")
    @Transactional
    public Observation resolveUrgent(UUID id) {
        Observation observation = getById(id);
        if (!observation.isUrgent()) {
            throw new BusinessRuleException("Observation is not flagged as urgent");
        }
        observation.setUrgentResolved(true);
        observation.setUrgentResolvedAt(Instant.now());
        observation.setUrgentResolvedBy(currentUsername());
        return observation;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
