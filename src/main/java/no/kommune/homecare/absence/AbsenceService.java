package no.kommune.homecare.absence;

import lombok.RequiredArgsConstructor;
import no.kommune.homecare.audit.AuditAction;
import no.kommune.homecare.audit.Auditable;
import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.common.exception.ResourceNotFoundException;
import no.kommune.homecare.absence.dto.AbsenceRequest;
import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.nurse.NurseService;
import no.kommune.homecare.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final NurseService nurseService;
    private final VisitRedistributionService redistributionService;

    @Auditable(action = AuditAction.CREATE, entityType = "Absence", details = "Nurse absence registered")
    @Transactional
    public Absence register(AbsenceRequest request) {
        if (!request.startDateTime().isBefore(request.endDateTime())) {
            throw new BusinessRuleException("Absence start must be before end");
        }
        Nurse nurse = nurseService.getById(request.nurseId());

        Absence absence = Absence.builder()
                .nurse(nurse)
                .startDateTime(request.startDateTime())
                .endDateTime(request.endDateTime())
                .reason(request.reason())
                .notes(request.notes())
                .redistributed(false)
                .build();

        Absence saved = absenceRepository.save(absence);
        redistributionService.redistribute(saved);
        return saved;
    }

    public Absence getById(UUID id) {
        Absence absence = absenceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Absence", id));
        CurrentUser.assertAccessible(absence.getNurse().getMunicipality(), absence.getNurse().getBydel());
        return absence;
    }

    public List<Absence> findByNurse(UUID nurseId) {
        // Throws if the nurse belongs to another kommune (or bydel) than the caller.
        nurseService.getById(nurseId);
        return absenceRepository.findByNurseId(nurseId);
    }

    public List<Absence> findAll() {
        return CurrentUser.municipality()
                .map(m -> absenceRepository.findAll().stream()
                        .filter(a -> m.equalsIgnoreCase(a.getNurse().getMunicipality()))
                        .filter(a -> CurrentUser.bydel().map(b -> b.equalsIgnoreCase(a.getNurse().getBydel())).orElse(true))
                        .toList())
                .orElseGet(absenceRepository::findAll);
    }
}
