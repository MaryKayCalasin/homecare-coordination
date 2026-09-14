package no.kommune.homecare.vedtak;

import lombok.RequiredArgsConstructor;
import no.kommune.homecare.audit.AuditAction;
import no.kommune.homecare.audit.Auditable;
import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.common.exception.ResourceNotFoundException;
import no.kommune.homecare.patient.Patient;
import no.kommune.homecare.patient.PatientService;
import no.kommune.homecare.security.CurrentUser;
import no.kommune.homecare.vedtak.dto.VedtakRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VedtakService {

    private final VedtakRepository vedtakRepository;
    private final PatientService patientService;

    @Auditable(action = AuditAction.CREATE, entityType = "Vedtak", details = "Vedtak registered")
    @Transactional
    public Vedtak create(VedtakRequest request) {
        // Throws if the patient belongs to another kommune than the caller.
        Patient patient = patientService.getById(request.patientId());

        if (request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
            throw new BusinessRuleException("validTo must not be before validFrom");
        }

        Vedtak vedtak = Vedtak.builder()
                .patient(patient)
                .serviceType(request.serviceType())
                .grantedHoursPerWeek(request.grantedHoursPerWeek())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .decidedBy(request.decidedBy())
                .decidedAt(Instant.now())
                .status(VedtakStatus.ACTIVE)
                .notes(request.notes())
                .build();

        return vedtakRepository.save(vedtak);
    }

    public Vedtak getById(UUID id) {
        Vedtak vedtak = vedtakRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Vedtak", id));
        CurrentUser.assertAccessible(vedtak.getPatient().getMunicipality());
        return vedtak;
    }

    public List<Vedtak> findByPatient(UUID patientId) {
        // Throws if the patient belongs to another kommune than the caller.
        patientService.getById(patientId);
        return vedtakRepository.findByPatientIdOrderByValidFromDesc(patientId);
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Vedtak", details = "Vedtak revoked")
    @Transactional
    public Vedtak revoke(UUID id) {
        Vedtak vedtak = getById(id);
        vedtak.setStatus(VedtakStatus.REVOKED);
        return vedtak;
    }
}
