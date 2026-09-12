package no.kommune.homecare.websocket.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RedistributionMessage(
        UUID absentNurseId,
        String absentNurseName,
        List<VisitReassignment> reassignments,
        List<UUID> unassignedVisitIds,
        Instant timestamp
) {
    public record VisitReassignment(UUID visitId, UUID newNurseId, String newNurseName) {
    }
}
