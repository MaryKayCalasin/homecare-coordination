package no.kommune.homecare.patient.dto;

import no.kommune.homecare.patient.CareLevel;
import no.kommune.homecare.patient.Patient;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String fullName,
        String nationalId,
        LocalDate dateOfBirth,
        String address,
        String postalCode,
        String city,
        String municipality,
        String phone,
        String nextOfKinName,
        String nextOfKinPhone,
        String primaryDiagnosis,
        CareLevel careLevel,
        String careNotes,
        boolean active,
        Double latitude,
        Double longitude
) {
    public static PatientResponse from(Patient p) {
        return new PatientResponse(
                p.getId(), p.getFullName(), p.getNationalId(), p.getDateOfBirth(), p.getAddress(),
                p.getPostalCode(), p.getCity(), p.getMunicipality(), p.getPhone(), p.getNextOfKinName(),
                p.getNextOfKinPhone(), p.getPrimaryDiagnosis(), p.getCareLevel(), p.getCareNotes(), p.isActive(),
                p.getLatitude(), p.getLongitude()
        );
    }
}
