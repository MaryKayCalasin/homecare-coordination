package no.kommune.homecare.websocket.dto;

import java.time.Instant;
import java.util.UUID;

public record UrgentAlertMessage(
        UUID observationId,
        UUID patientId,
        String patientName,
        UUID recordedByNurseId,
        String recordedByNurseName,
        String description,
        Instant timestamp
) {
}
