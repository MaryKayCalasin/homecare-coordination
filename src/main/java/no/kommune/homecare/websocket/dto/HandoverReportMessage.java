package no.kommune.homecare.websocket.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HandoverReportMessage(
        UUID reportId,
        LocalDate shiftDate,
        String shiftType,
        int urgentObservationsCount,
        Instant timestamp
) {
}
