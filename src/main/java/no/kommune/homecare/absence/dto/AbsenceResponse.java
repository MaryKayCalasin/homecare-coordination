package no.kommune.homecare.absence.dto;

import no.kommune.homecare.absence.Absence;
import no.kommune.homecare.absence.AbsenceReason;

import java.time.Instant;
import java.util.UUID;

public record AbsenceResponse(
        UUID id,
        UUID nurseId,
        String nurseName,
        Instant startDateTime,
        Instant endDateTime,
        AbsenceReason reason,
        String notes,
        boolean redistributed
) {
    public static AbsenceResponse from(Absence a) {
        return new AbsenceResponse(a.getId(), a.getNurse().getId(), a.getNurse().getFullName(),
                a.getStartDateTime(), a.getEndDateTime(), a.getReason(), a.getNotes(), a.isRedistributed());
    }
}
