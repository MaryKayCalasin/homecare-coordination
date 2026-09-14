package no.kommune.homecare.vedtak;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.kommune.homecare.vedtak.dto.VedtakRequest;
import no.kommune.homecare.vedtak.dto.VedtakResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The entitlement a patient's visits are supposed to trace back to. Nothing
 * here yet enforces that a visit stays within its vedtak's granted hours -
 * see the class Javadoc on {@link Vedtak} for what this is and isn't.
 */
@RestController
@RequestMapping("/api/vedtak")
@RequiredArgsConstructor
public class VedtakController {

    private final VedtakService vedtakService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<VedtakResponse> create(@Valid @RequestBody VedtakRequest request) {
        Vedtak vedtak = vedtakService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(VedtakResponse.from(vedtak));
    }

    @GetMapping("/{id}")
    public VedtakResponse getById(@PathVariable UUID id) {
        return VedtakResponse.from(vedtakService.getById(id));
    }

    @GetMapping("/by-patient/{patientId}")
    public List<VedtakResponse> byPatient(@PathVariable UUID patientId) {
        return vedtakService.findByPatient(patientId).stream().map(VedtakResponse::from).toList();
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public VedtakResponse revoke(@PathVariable UUID id) {
        return VedtakResponse.from(vedtakService.revoke(id));
    }
}
