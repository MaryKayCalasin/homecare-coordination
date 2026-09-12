package no.kommune.homecare.observation.dto;

import no.kommune.homecare.observation.MoodLevel;
import no.kommune.homecare.observation.Observation;
import no.kommune.homecare.observation.ObservationType;

import java.time.Instant;
import java.util.UUID;

public record ObservationResponse(
        UUID id,
        UUID patientId,
        String patientName,
        UUID recordedByNurseId,
        String recordedByNurseName,
        UUID visitId,
        ObservationType observationType,
        String medicationName,
        String medicationDosage,
        Boolean medicationGiven,
        MoodLevel moodLevel,
        String description,
        boolean urgent,
        boolean urgentResolved,
        Instant urgentResolvedAt,
        Instant recordedAt
) {
    public static ObservationResponse from(Observation o) {
        return new ObservationResponse(
                o.getId(),
                o.getPatient().getId(),
                o.getPatient().getFullName(),
                o.getRecordedBy().getId(),
                o.getRecordedBy().getFullName(),
                o.getVisit() == null ? null : o.getVisit().getId(),
                o.getObservationType(),
                o.getMedicationName(),
                o.getMedicationDosage(),
                o.getMedicationGiven(),
                o.getMoodLevel(),
                o.getDescription(),
                o.isUrgent(),
                o.isUrgentResolved(),
                o.getUrgentResolvedAt(),
                o.getRecordedAt()
        );
    }
}
