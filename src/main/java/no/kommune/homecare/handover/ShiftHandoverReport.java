package no.kommune.homecare.handover;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.kommune.homecare.common.entity.BaseEntity;

import java.time.LocalDate;

/**
 * An auto-generated summary of everything the outgoing shift needs to hand
 * over to the next one: visits completed or missed, medications given,
 * mood/wellbeing notes and any unresolved urgent flags.
 */
@Getter
@Setter
@Entity
@Table(name = "shift_handover_report")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class ShiftHandoverReport extends BaseEntity {

    @Column(nullable = false)
    private LocalDate shiftDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftType shiftType;

    @Column(nullable = false, length = 100)
    private String municipality;

    @Column(nullable = false, length = 100)
    private String generatedBy;

    @Column(nullable = false)
    private int visitsCompletedCount;

    @Column(nullable = false)
    private int visitsMissedCount;

    @Column(nullable = false)
    private int visitsCancelledCount;

    @Column(nullable = false)
    private int visitsPendingCount;

    @Column(nullable = false)
    private int observationsCount;

    @Column(nullable = false)
    private int urgentObservationsCount;

    @Column(nullable = false)
    private int unresolvedUrgentCount;

    @Lob
    @Column(nullable = false)
    private String summary;
}
