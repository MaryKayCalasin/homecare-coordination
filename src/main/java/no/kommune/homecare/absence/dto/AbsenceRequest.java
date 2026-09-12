package no.kommune.homecare.absence.dto;

import jakarta.validation.constraints.NotNull;
import no.kommune.homecare.absence.AbsenceReason;

import java.time.Instant;
import java.util.UUID;

public record AbsenceRequest(
        @NotNull UUID nurseId,
        @NotNull Instant startDateTime,
        @NotNull Instant endDateTime,
        @NotNull AbsenceReason reason,
        String notes
) {
}
