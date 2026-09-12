package no.kommune.homecare.absence;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.kommune.homecare.absence.dto.AbsenceRequest;
import no.kommune.homecare.absence.dto.AbsenceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/absences")
@RequiredArgsConstructor
public class AbsenceController {

    private final AbsenceService absenceService;

    /**
     * Registers a nurse absence and immediately triggers automatic
     * redistribution of every visit that nurse was scheduled to carry out
     * during the absence window.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'NURSE')")
    public ResponseEntity<AbsenceResponse> register(@Valid @RequestBody AbsenceRequest request) {
        Absence absence = absenceService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AbsenceResponse.from(absence));
    }

    @GetMapping("/{id}")
    public AbsenceResponse getById(@PathVariable UUID id) {
        return AbsenceResponse.from(absenceService.getById(id));
    }

    @GetMapping
    public List<AbsenceResponse> findAll() {
        return absenceService.findAll().stream().map(AbsenceResponse::from).toList();
    }

    @GetMapping("/by-nurse/{nurseId}")
    public List<AbsenceResponse> findByNurse(@PathVariable UUID nurseId) {
        return absenceService.findByNurse(nurseId).stream().map(AbsenceResponse::from).toList();
    }
}
