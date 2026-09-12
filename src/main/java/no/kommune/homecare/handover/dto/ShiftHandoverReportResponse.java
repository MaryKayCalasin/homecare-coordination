package no.kommune.homecare.handover.dto;

import no.kommune.homecare.handover.ShiftHandoverReport;
import no.kommune.homecare.handover.ShiftType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ShiftHandoverReportResponse(
        UUID id,
        LocalDate shiftDate,
        ShiftType shiftType,
        String municipality,
        String generatedBy,
        int visitsCompletedCount,
        int visitsMissedCount,
        int visitsCancelledCount,
        int visitsPendingCount,
        int observationsCount,
        int urgentObservationsCount,
        int unresolvedUrgentCount,
        String summary,
        Instant createdAt
) {
    public static ShiftHandoverReportResponse from(ShiftHandoverReport r) {
        return new ShiftHandoverReportResponse(
                r.getId(), r.getShiftDate(), r.getShiftType(), r.getMunicipality(), r.getGeneratedBy(),
                r.getVisitsCompletedCount(), r.getVisitsMissedCount(), r.getVisitsCancelledCount(),
                r.getVisitsPendingCount(), r.getObservationsCount(), r.getUrgentObservationsCount(),
                r.getUnresolvedUrgentCount(), r.getSummary(), r.getCreatedAt()
        );
    }
}
