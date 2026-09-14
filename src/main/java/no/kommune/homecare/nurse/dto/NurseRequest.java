package no.kommune.homecare.nurse.dto;

import jakarta.validation.constraints.NotBlank;
import no.kommune.homecare.nurse.Qualification;

import java.util.Set;
import java.util.UUID;

public record NurseRequest(
        @NotBlank String fullName,
        @NotBlank String employeeId,
        String phone,
        String email,
        @NotBlank String municipality,
        String bydel,
        UUID userId,
        Set<Qualification> qualifications
) {
}
