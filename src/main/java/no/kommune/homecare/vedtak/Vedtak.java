package no.kommune.homecare.vedtak;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.kommune.homecare.common.entity.BaseEntity;
import no.kommune.homecare.patient.Patient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An administrative decision ("vedtak") granting a patient a specific home
 * care service under helse- og omsorgstjenesteloven. In a real kommune,
 * every visit exists because some vedtak entitles the patient to it - a
 * nurse doesn't just show up; the municipality has formally decided the
 * patient qualifies for N hours per week of a specific service, for a
 * bounded period, and that decision is what gets reported (in aggregate,
 * anonymized) to IPLOS every year.
 * <p>
 * This is a foundation, not a complete entitlement system: nothing here yet
 * tracks hours actually delivered against {@link #grantedHoursPerWeek}, and
 * there's no IPLOS export. {@link no.kommune.homecare.visit.Visit} can
 * optionally reference the vedtak it was scheduled under, which is enough
 * to answer "which decision justifies this visit" - consuming that link to
 * flag over- or under-delivery is future work.
 */
@Getter
@Setter
@Entity
@Table(name = "vedtak")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Vedtak extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IplosServiceType serviceType;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal grantedHoursPerWeek;

    @Column(nullable = false)
    private LocalDate validFrom;

    /** Open-ended if null - some vedtak (e.g. long-term stay) have no fixed end date. */
    private LocalDate validTo;

    @Column(nullable = false, length = 150)
    private String decidedBy;

    @Column(nullable = false)
    private Instant decidedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private VedtakStatus status = VedtakStatus.ACTIVE;

    @Column(length = 1000)
    private String notes;

    public boolean isInEffectOn(LocalDate date) {
        if (status != VedtakStatus.ACTIVE) {
            return false;
        }
        if (date.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !date.isAfter(validTo);
    }
}
