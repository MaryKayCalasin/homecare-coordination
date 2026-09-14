package no.kommune.homecare.visit.dto;

import no.kommune.homecare.visit.Visit;
import no.kommune.homecare.visit.VisitStatus;
import no.kommune.homecare.visit.VisitType;

import java.time.Instant;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID patientId,
        String patientName,
        UUID nurseId,
        String nurseName,
        Instant scheduledStart,
        Instant scheduledEnd,
        Instant actualStart,
        Instant actualEnd,
        VisitStatus status,
        VisitType visitType,
        String notes,
        String location,
        UUID vedtakId
) {
    public static VisitResponse from(Visit v) {
        return new VisitResponse(
                v.getId(),
                v.getPatient().getId(),
                v.getPatient().getFullName(),
                v.getNurse() == null ? null : v.getNurse().getId(),
                v.getNurse() == null ? null : v.getNurse().getFullName(),
                v.getScheduledStart(),
                v.getScheduledEnd(),
                v.getActualStart(),
                v.getActualEnd(),
                v.getStatus(),
                v.getVisitType(),
                v.getNotes(),
                v.getLocation(),
                v.getVedtak() == null ? null : v.getVedtak().getId()
        );
    }
}
