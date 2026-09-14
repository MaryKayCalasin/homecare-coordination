package no.kommune.homecare.absence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.kommune.homecare.audit.AuditAction;
import no.kommune.homecare.audit.AuditService;
import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.nurse.NurseRepository;
import no.kommune.homecare.visit.TravelTime;
import no.kommune.homecare.visit.Visit;
import no.kommune.homecare.visit.VisitCapability;
import no.kommune.homecare.visit.VisitRepository;
import no.kommune.homecare.visit.VisitService;
import no.kommune.homecare.visit.VisitStatus;
import no.kommune.homecare.websocket.WebSocketEventPublisher;
import no.kommune.homecare.websocket.dto.RedistributionMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * When a nurse becomes absent, every visit they were scheduled to carry out
 * during the absence window must be picked up by someone else so that no
 * patient is left without care. This service finds those visits and
 * distributes them across the remaining available nurses in the same
 * municipality, balancing load and avoiding double-booking. Any visit that
 * cannot be covered automatically is left unassigned for a coordinator to
 * resolve by hand, and everyone is notified in real time over WebSocket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitRedistributionService {

    private final VisitRepository visitRepository;
    private final NurseRepository nurseRepository;
    private final AbsenceRepository absenceRepository;
    private final VisitService visitService;
    private final WebSocketEventPublisher eventPublisher;
    private final AuditService auditService;

    @Transactional
    public RedistributionMessage redistribute(Absence absence) {
        Nurse absentNurse = absence.getNurse();

        List<Visit> affectedVisits = visitRepository
                .findByNurseIdAndStatusAndScheduledStartBetween(
                        absentNurse.getId(), VisitStatus.SCHEDULED, absence.getStartDateTime(), absence.getEndDateTime())
                .stream()
                .sorted(Comparator.comparing(Visit::getScheduledStart))
                .toList();

        List<Nurse> candidates = nurseRepository
                .findByMunicipalityIgnoreCaseAndActiveTrue(absentNurse.getMunicipality())
                .stream()
                .filter(n -> !n.getId().equals(absentNurse.getId()))
                .toList();

        // Running count of visits each candidate already carries in the window,
        // used to spread the extra load evenly rather than piling onto one nurse.
        Map<UUID, Long> loadByNurse = candidates.stream()
                .collect(Collectors.toMap(Nurse::getId, n -> (long)
                        visitRepository.findByNurseIdAndScheduledStartBetween(
                                n.getId(), absence.getStartDateTime(), absence.getEndDateTime()).size()));

        List<RedistributionMessage.VisitReassignment> reassignments = new ArrayList<>();
        List<UUID> unassignedVisitIds = new ArrayList<>();

        for (Visit visit : affectedVisits) {
            Nurse substitute = pickSubstitute(candidates, loadByNurse, visit, absence);
            if (substitute == null) {
                visitService.unassign(visit.getId());
                unassignedVisitIds.add(visit.getId());
                log.warn("No available substitute nurse found for visit {} during absence of nurse {}",
                        visit.getId(), absentNurse.getId());
                continue;
            }
            visitService.assignNurse(visit.getId(), substitute.getId());
            loadByNurse.merge(substitute.getId(), 1L, Long::sum);
            reassignments.add(new RedistributionMessage.VisitReassignment(
                    visit.getId(), substitute.getId(), substitute.getFullName()));
        }

        absence.setRedistributed(true);

        auditService.record(AuditAction.REASSIGN, "Absence", absence.getId(),
                "Redistributed %d visit(s) for absent nurse %s: %d reassigned, %d unassigned"
                        .formatted(affectedVisits.size(), absentNurse.getFullName(),
                                reassignments.size(), unassignedVisitIds.size()));

        RedistributionMessage message = new RedistributionMessage(
                absentNurse.getId(), absentNurse.getFullName(), reassignments, unassignedVisitIds, Instant.now());
        eventPublisher.publishRedistribution(message);
        return message;
    }

    /**
     * Picks a substitute from whoever is available and qualified for this
     * visit's type, preferring a nurse who has cared for this patient
     * before (continuity of care - "kontaktsykepleier" in Norwegian home
     * care) over a stranger, and breaking ties within that preferred group
     * by current load. Falls back to the full eligible pool, still
     * least-loaded first, if nobody with prior history is available.
     */
    private Nurse pickSubstitute(List<Nurse> candidates, Map<UUID, Long> loadByNurse, Visit visit, Absence absence) {
        List<Nurse> eligible = candidates.stream()
                .filter(candidate -> isAvailable(candidate, visit, absence))
                .filter(candidate -> VisitCapability.isQualified(candidate, visit.getVisitType()))
                .toList();

        List<Nurse> withHistory = eligible.stream()
                .filter(candidate -> visitRepository.existsByNurseIdAndPatientIdAndStatusNot(
                        candidate.getId(), visit.getPatient().getId(), VisitStatus.CANCELLED))
                .toList();

        List<Nurse> pool = withHistory.isEmpty() ? eligible : withHistory;

        return pool.stream()
                .min(Comparator.comparingLong(n -> loadByNurse.getOrDefault(n.getId(), 0L)))
                .orElse(null);
    }

    private boolean isAvailable(Nurse candidate, Visit visit, Absence absence) {
        boolean isAlsoAbsent = !absenceRepository
                .findByNurseIdAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
                        candidate.getId(), visit.getScheduledEnd(), visit.getScheduledStart())
                .isEmpty();
        if (isAlsoAbsent) {
            return false;
        }

        List<Visit> nearby = visitRepository
                .findByNurseIdAndScheduledStartBetween(
                        candidate.getId(),
                        visit.getScheduledStart().minusSeconds(24 * 3600),
                        visit.getScheduledEnd().plusSeconds(24 * 3600))
                .stream()
                .filter(v -> v.getStatus() != VisitStatus.CANCELLED)
                .toList();

        boolean hasConflict = nearby.stream().anyMatch(v -> v.overlaps(visit.getScheduledStart(), visit.getScheduledEnd()));
        if (hasConflict) {
            return false;
        }

        return TravelTime.isFeasible(nearby, visit.getPatient(), visit.getScheduledStart(), visit.getScheduledEnd());
    }
}
