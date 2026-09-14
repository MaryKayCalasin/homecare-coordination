package no.kommune.homecare.patient;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.kommune.homecare.patient.dto.PatientRequest;
import no.kommune.homecare.patient.dto.PatientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request) {
        Patient patient = patientService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientResponse.from(patient));
    }

    @GetMapping("/{id}")
    public PatientResponse getById(@PathVariable UUID id) {
        return PatientResponse.from(patientService.getById(id));
    }

    @GetMapping
    public Page<PatientResponse> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String municipality,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Patient> page;
        if (name != null && !name.isBlank()) {
            page = patientService.search(name, pageable);
        } else if (municipality != null && !municipality.isBlank()) {
            page = patientService.findByMunicipality(municipality, pageable);
        } else {
            page = patientService.findAll(pageable);
        }
        return page.map(PatientResponse::from);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public PatientResponse update(@PathVariable UUID id, @Valid @RequestBody PatientRequest request) {
        return PatientResponse.from(patientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        patientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Caches the result of the frontend's client-side Nominatim geocoding
     * lookup for this patient's address, so visit scheduling can check
     * travel time between consecutive visits.
     */
    @PatchMapping("/{id}/coordinates")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'NURSE')")
    public PatientResponse updateCoordinates(@PathVariable UUID id,
                                              @RequestParam double latitude,
                                              @RequestParam double longitude) {
        return PatientResponse.from(patientService.updateCoordinates(id, latitude, longitude));
    }
}
