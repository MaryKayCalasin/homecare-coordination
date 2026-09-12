package no.kommune.homecare.nurse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.kommune.homecare.nurse.dto.NurseRequest;
import no.kommune.homecare.nurse.dto.NurseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nurses")
@RequiredArgsConstructor
public class NurseController {

    private final NurseService nurseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<NurseResponse> create(@Valid @RequestBody NurseRequest request) {
        Nurse nurse = nurseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(NurseResponse.from(nurse));
    }

    @GetMapping("/{id}")
    public NurseResponse getById(@PathVariable UUID id) {
        return NurseResponse.from(nurseService.getById(id));
    }

    @GetMapping
    public Page<NurseResponse> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return nurseService.findAll(pageable).map(NurseResponse::from);
    }

    @GetMapping("/by-municipality")
    public List<NurseResponse> findByMunicipality(@RequestParam String municipality) {
        return nurseService.findByMunicipality(municipality).stream().map(NurseResponse::from).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public NurseResponse update(@PathVariable UUID id, @Valid @RequestBody NurseRequest request) {
        return NurseResponse.from(nurseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        nurseService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
