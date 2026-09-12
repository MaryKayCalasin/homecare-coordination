package no.kommune.homecare.nurse.dto;

import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.nurse.Qualification;

import java.util.Set;
import java.util.UUID;

public record NurseResponse(
        UUID id,
        String fullName,
        String employeeId,
        String phone,
        String email,
        String municipality,
        Set<Qualification> qualifications,
        boolean active,
        boolean canGiveMedication,
        boolean canDoWoundCare,
        boolean canDoIV,
        boolean canLiftHeavy,
        boolean canWorkAlone,
        boolean canHandleDementia,
        boolean canDoPersonalCare
) {
    public static NurseResponse from(Nurse n) {
        return new NurseResponse(n.getId(), n.getFullName(), n.getEmployeeId(), n.getPhone(), n.getEmail(),
                n.getMunicipality(), n.getQualifications(), n.isActive(),
                n.isCanGiveMedication(), n.isCanDoWoundCare(), n.isCanDoIV(), n.isCanLiftHeavy(),
                n.isCanWorkAlone(), n.isCanHandleDementia(), n.isCanDoPersonalCare());
    }
}
