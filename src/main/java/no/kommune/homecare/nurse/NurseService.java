package no.kommune.homecare.nurse;

import lombok.RequiredArgsConstructor;
import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.common.exception.ResourceNotFoundException;
import no.kommune.homecare.nurse.dto.NurseRequest;
import no.kommune.homecare.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NurseService {

    private final NurseRepository nurseRepository;

    @Transactional
    public Nurse create(NurseRequest request) {
        if (nurseRepository.existsByEmployeeId(request.employeeId())) {
            throw new BusinessRuleException("A nurse with employee ID " + request.employeeId() + " already exists");
        }
        CurrentUser.assertAccessible(request.municipality(), request.bydel());
        Nurse nurse = Nurse.builder()
                .fullName(request.fullName())
                .employeeId(request.employeeId())
                .phone(request.phone())
                .email(request.email())
                .municipality(request.municipality())
                .bydel(request.bydel())
                .userId(request.userId())
                .qualifications(request.qualifications() == null ? new HashSet<>() : new HashSet<>(request.qualifications()))
                .active(true)
                .build();
        return nurseRepository.save(nurse);
    }

    public Nurse getById(UUID id) {
        Nurse nurse = nurseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Nurse", id));
        CurrentUser.assertAccessible(nurse.getMunicipality(), nurse.getBydel());
        return nurse;
    }

    public Page<Nurse> findAll(Pageable pageable) {
        return CurrentUser.municipality()
                .map(m -> CurrentUser.bydel()
                        .map(b -> nurseRepository.findPageByMunicipalityIgnoreCaseAndBydelIgnoreCaseAndActiveTrue(m, b, pageable))
                        .orElseGet(() -> nurseRepository.findPageByMunicipalityIgnoreCaseAndActiveTrue(m, pageable)))
                .orElseGet(() -> nurseRepository.findByActiveTrue(pageable));
    }

    public List<Nurse> findByMunicipality(String municipality) {
        CurrentUser.assertAccessible(municipality);
        return CurrentUser.bydel()
                .map(b -> nurseRepository.findByMunicipalityIgnoreCaseAndBydelIgnoreCaseAndActiveTrue(municipality, b))
                .orElseGet(() -> nurseRepository.findByMunicipalityIgnoreCaseAndActiveTrue(municipality));
    }

    @Transactional
    public Nurse update(UUID id, NurseRequest request) {
        Nurse nurse = getById(id);
        CurrentUser.assertAccessible(request.municipality(), request.bydel());
        nurse.setFullName(request.fullName());
        nurse.setPhone(request.phone());
        nurse.setEmail(request.email());
        nurse.setMunicipality(request.municipality());
        nurse.setBydel(request.bydel());
        nurse.setUserId(request.userId());
        if (request.qualifications() != null) {
            nurse.setQualifications(new HashSet<>(request.qualifications()));
        }
        return nurse;
    }

    @Transactional
    public void deactivate(UUID id) {
        Nurse nurse = getById(id);
        nurse.setActive(false);
    }
}
