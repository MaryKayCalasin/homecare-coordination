package no.kommune.homecare.vedtak.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.kommune.homecare.vedtak.IplosServiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VedtakRequest(
        @NotNull UUID patientId,
        @NotNull IplosServiceType serviceType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal grantedHoursPerWeek,
        @NotNull LocalDate validFrom,
        LocalDate validTo,
        @NotBlank String decidedBy,
        String notes
) {
}
