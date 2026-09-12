package no.kommune.homecare.patient;

import lombok.RequiredArgsConstructor;
import no.kommune.homecare.audit.AuditAction;
import no.kommune.homecare.audit.Auditable;
import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.common.exception.ResourceNotFoundException;
import no.kommune.homecare.patient.dto.PatientRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;

    @Auditable(action = AuditAction.CREATE, entityType = "Patient", details = "Patient record created")
    @Transactional
    public Patient create(PatientRequest request) {
        if (patientRepository.existsByNationalId(request.nationalId())) {
            throw new BusinessRuleException("A patient with this national ID already exists");
        }
        Patient patient = Patient.builder()
                .fullName(request.fullName())
                .nationalId(request.nationalId())
                .dateOfBirth(request.dateOfBirth())
                .address(request.address())
                .postalCode(request.postalCode())
                .city(request.city())
                .municipality(request.municipality())
                .phone(request.phone())
                .nextOfKinName(request.nextOfKinName())
                .nextOfKinPhone(request.nextOfKinPhone())
                .primaryDiagnosis(request.primaryDiagnosis())
                .careLevel(request.careLevel())
                .careNotes(request.careNotes())
                .active(true)
                .build();
        return patientRepository.save(patient);
    }

    @Auditable(action = AuditAction.READ, entityType = "Patient", details = "Patient record accessed")
    public Patient getById(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Patient", id));
    }

    public Page<Patient> findAll(Pageable pageable) {
        return patientRepository.findByActiveTrue(pageable);
    }

    public Page<Patient> search(String name, Pageable pageable) {
        return patientRepository.findByFullNameContainingIgnoreCase(name, pageable);
    }

    public Page<Patient> findByMunicipality(String municipality, Pageable pageable) {
        return patientRepository.findByMunicipalityIgnoreCaseAndActiveTrue(municipality, pageable);
    }

    public List<String> findActiveMunicipalities() {
        return patientRepository.findDistinctActiveMunicipalities();
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Patient", details = "Patient record updated")
    @Transactional
    public Patient update(UUID id, PatientRequest request) {
        Patient patient = getById(id);
        patient.setFullName(request.fullName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setAddress(request.address());
        patient.setPostalCode(request.postalCode());
        patient.setCity(request.city());
        patient.setMunicipality(request.municipality());
        patient.setPhone(request.phone());
        patient.setNextOfKinName(request.nextOfKinName());
        patient.setNextOfKinPhone(request.nextOfKinPhone());
        patient.setPrimaryDiagnosis(request.primaryDiagnosis());
        patient.setCareLevel(request.careLevel());
        patient.setCareNotes(request.careNotes());
        return patient;
    }

    @Auditable(action = AuditAction.DELETE, entityType = "Patient", details = "Patient record deactivated")
    @Transactional
    public void deactivate(UUID id) {
        Patient patient = getById(id);
        patient.setActive(false);
    }
}
