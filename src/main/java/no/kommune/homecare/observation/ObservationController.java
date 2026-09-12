package no.kommune.homecare.observation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.kommune.homecare.observation.dto.ObservationRequest;
import no.kommune.homecare.observation.dto.ObservationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/observations")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService observationService;

    @PostMapping
    public ResponseEntity<ObservationResponse> create(@Valid @RequestBody ObservationRequest request) {
        Observation observation = observationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ObservationResponse.from(observation));
    }

    @GetMapping("/{id}")
    public ObservationResponse getById(@PathVariable UUID id) {
        return ObservationResponse.from(observationService.getById(id));
    }

    @GetMapping("/by-patient/{patientId}")
    public Page<ObservationResponse> byPatient(@PathVariable UUID patientId,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return observationService.findByPatient(patientId, pageable).map(ObservationResponse::from);
    }

    @GetMapping("/urgent")
    public List<ObservationResponse> urgentUnresolved() {
        return observationService.findUnresolvedUrgent().stream().map(ObservationResponse::from).toList();
    }

    @PatchMapping("/{id}/resolve-urgent")
    public ObservationResponse resolveUrgent(@PathVariable UUID id) {
        return ObservationResponse.from(observationService.resolveUrgent(id));
    }
}
