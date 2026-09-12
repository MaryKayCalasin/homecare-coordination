package no.kommune.homecare.visit;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.kommune.homecare.visit.dto.VisitRequest;
import no.kommune.homecare.visit.dto.VisitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private static final ZoneId OSLO = ZoneId.of("Europe/Oslo");

    private final VisitService visitService;

    /**
     * Lists every visit scheduled on a given day (defaults to today, Europe/Oslo),
     * across all patients and nurses - used by the coordinator dashboard and map view.
     */
    @GetMapping
    public List<VisitResponse> byDate(@RequestParam(required = false) LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now(OSLO);
        Instant start = LocalDateTime.of(day, LocalTime.MIN).atZone(OSLO).toInstant();
        Instant end = LocalDateTime.of(day.plusDays(1), LocalTime.MIN).atZone(OSLO).toInstant();
        return visitService.findByRange(start, end).stream().map(VisitResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<VisitResponse> create(@Valid @RequestBody VisitRequest request) {
        Visit visit = visitService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(VisitResponse.from(visit));
    }

    @GetMapping("/{id}")
    public VisitResponse getById(@PathVariable UUID id) {
        return VisitResponse.from(visitService.getById(id));
    }

    @GetMapping("/by-patient/{patientId}")
    public Page<VisitResponse> byPatient(@PathVariable UUID patientId, @PageableDefault(size = 20) Pageable pageable) {
        return visitService.findByPatient(patientId, pageable).map(VisitResponse::from);
    }

    @GetMapping("/by-nurse/{nurseId}")
    public Page<VisitResponse> byNurse(@PathVariable UUID nurseId, @PageableDefault(size = 20) Pageable pageable) {
        return visitService.findByNurse(nurseId, pageable).map(VisitResponse::from);
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public VisitResponse reschedule(@PathVariable UUID id,
                                     @RequestParam Instant start,
                                     @RequestParam Instant end) {
        return VisitResponse.from(visitService.reschedule(id, start, end));
    }

    @PutMapping("/{id}/assign-nurse/{nurseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public VisitResponse assignNurse(@PathVariable UUID id, @PathVariable UUID nurseId) {
        return VisitResponse.from(visitService.assignNurse(id, nurseId));
    }

    @PatchMapping("/{id}/start")
    public VisitResponse start(@PathVariable UUID id) {
        return VisitResponse.from(visitService.start(id));
    }

    @PatchMapping("/{id}/complete")
    public VisitResponse complete(@PathVariable UUID id, @RequestParam(required = false) String notes) {
        return VisitResponse.from(visitService.complete(id, notes));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public VisitResponse cancel(@PathVariable UUID id, @RequestParam(required = false) String reason) {
        return VisitResponse.from(visitService.cancel(id, reason));
    }

    @PatchMapping("/{id}/missed")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public VisitResponse markMissed(@PathVariable UUID id) {
        return VisitResponse.from(visitService.markMissed(id));
    }
}
