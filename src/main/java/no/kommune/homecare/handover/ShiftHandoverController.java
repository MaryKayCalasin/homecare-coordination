package no.kommune.homecare.handover;

import lombok.RequiredArgsConstructor;
import no.kommune.homecare.handover.dto.ShiftHandoverReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/handover-reports")
@RequiredArgsConstructor
public class ShiftHandoverController {

    private final ShiftHandoverService shiftHandoverService;

    /**
     * Manually generates the handover report for a specific shift, in
     * addition to the reports the platform generates automatically at every
     * shift boundary.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'NURSE')")
    public ResponseEntity<ShiftHandoverReportResponse> generate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate,
            @RequestParam ShiftType shiftType,
            @RequestParam String municipality) {
        ShiftHandoverReport report = shiftHandoverService.generate(shiftDate, shiftType, municipality);
        return ResponseEntity.status(HttpStatus.CREATED).body(ShiftHandoverReportResponse.from(report));
    }

    @GetMapping("/{id}")
    public ShiftHandoverReportResponse getById(@PathVariable UUID id) {
        return ShiftHandoverReportResponse.from(shiftHandoverService.getById(id));
    }

    @GetMapping("/by-municipality")
    public List<ShiftHandoverReportResponse> byMunicipality(@RequestParam String municipality) {
        return shiftHandoverService.findByMunicipality(municipality).stream()
                .map(ShiftHandoverReportResponse::from).toList();
    }

    @GetMapping
    public Page<ShiftHandoverReportResponse> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return shiftHandoverService.findAll(pageable).map(ShiftHandoverReportResponse::from);
    }
}
