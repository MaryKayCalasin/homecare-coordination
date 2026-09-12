package no.kommune.homecare.websocket.dto;

import java.time.Instant;
import java.util.UUID;

public record VisitUpdateMessage(
        UUID visitId,
        UUID patientId,
        UUID nurseId,
        String status,
        String eventType,
        String message,
        Instant timestamp
) {
    public static VisitUpdateMessage of(UUID visitId, UUID patientId, UUID nurseId, String status,
                                         String eventType, String message) {
        return new VisitUpdateMessage(visitId, patientId, nurseId, status, eventType, message, Instant.now());
    }
}
