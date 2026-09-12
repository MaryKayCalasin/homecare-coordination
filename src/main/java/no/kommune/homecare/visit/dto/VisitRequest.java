package no.kommune.homecare.visit.dto;

import jakarta.validation.constraints.NotNull;
import no.kommune.homecare.visit.VisitType;

import java.time.Instant;
import java.util.UUID;

public record VisitRequest(
        @NotNull UUID patientId,
        UUID nurseId,
        @NotNull Instant scheduledStart,
        @NotNull Instant scheduledEnd,
        @NotNull VisitType visitType,
        String notes,
        String location
) {
}
