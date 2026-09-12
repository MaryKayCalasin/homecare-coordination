package no.kommune.homecare.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only access to the GDPR audit trail. Restricted to administrators and
 * coordinators, who are responsible for demonstrating compliance.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public Page<AuditLog> findAll(
            @RequestParam(required = false) String entityType,
            @PageableDefault(size = 50) Pageable pageable) {
        if (entityType != null && !entityType.isBlank()) {
            return auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);
        }
        return auditLogRepository.findAll(pageable);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public Page<AuditLog> findForEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @PageableDefault(size = 50) Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
    }

    @GetMapping("/user/{username}")
    public Page<AuditLog> findForUser(
            @PathVariable String username,
            @PageableDefault(size = 50) Pageable pageable) {
        return auditLogRepository.findByPerformedByOrderByCreatedAtDesc(username, pageable);
    }
}
