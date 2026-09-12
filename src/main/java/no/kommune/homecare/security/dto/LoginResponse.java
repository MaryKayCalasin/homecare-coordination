package no.kommune.homecare.security.dto;

import java.util.UUID;

public record LoginResponse(
        String token,
        UUID userId,
        String username,
        String fullName,
        String role
) {
}
