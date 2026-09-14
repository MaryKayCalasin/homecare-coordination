package no.kommune.homecare.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import no.kommune.homecare.patient.CareLevel;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank String fullName,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "must be 11 digits") String nationalId,
        LocalDate dateOfBirth,
        String address,
        String postalCode,
        String city,
        @NotBlank String municipality,
        String phone,
        String nextOfKinName,
        String nextOfKinPhone,
        String primaryDiagnosis,
        @NotNull CareLevel careLevel,
        String careNotes,
        Double latitude,
        Double longitude
) {
}
