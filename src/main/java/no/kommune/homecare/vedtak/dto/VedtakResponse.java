package no.kommune.homecare.vedtak.dto;

import no.kommune.homecare.vedtak.IplosServiceType;
import no.kommune.homecare.vedtak.Vedtak;
import no.kommune.homecare.vedtak.VedtakStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VedtakResponse(
        UUID id,
        UUID patientId,
        String patientName,
        IplosServiceType serviceType,
        BigDecimal grantedHoursPerWeek,
        LocalDate validFrom,
        LocalDate validTo,
        String decidedBy,
        Instant decidedAt,
        VedtakStatus status,
        String notes
) {
    public static VedtakResponse from(Vedtak v) {
        return new VedtakResponse(
                v.getId(), v.getPatient().getId(), v.getPatient().getFullName(),
                v.getServiceType(), v.getGrantedHoursPerWeek(), v.getValidFrom(), v.getValidTo(),
                v.getDecidedBy(), v.getDecidedAt(), v.getStatus(), v.getNotes()
        );
    }
}
