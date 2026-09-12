package no.kommune.homecare.observation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.kommune.homecare.observation.MoodLevel;
import no.kommune.homecare.observation.ObservationType;

import java.util.UUID;

public record ObservationRequest(
        @NotNull UUID patientId,
        @NotNull UUID recordedByNurseId,
        UUID visitId,
        @NotNull ObservationType observationType,
        String medicationName,
        String medicationDosage,
        Boolean medicationGiven,
        MoodLevel moodLevel,
        @NotBlank String description,
        boolean urgent
) {
}
