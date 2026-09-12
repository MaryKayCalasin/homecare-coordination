package no.kommune.homecare.visit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.patient.Patient;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "visit", indexes = {
        @Index(name = "idx_visit_nurse_start", columnList = "nurse_id,scheduledStart"),
        @Index(name = "idx_visit_patient_start", columnList = "patient_id,scheduledStart")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Visit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** Nullable while a visit is awaiting reassignment after a nurse absence. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id")
    private Nurse nurse;

    @Column(nullable = false)
    private Instant scheduledStart;

    @Column(nullable = false)
    private Instant scheduledEnd;

    private Instant actualStart;

    private Instant actualEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VisitType visitType;

    @Column(length = 1000)
    private String notes;

    @Column(length = 200)
    private String location;

    public boolean overlaps(Instant start, Instant end) {
        return scheduledStart.isBefore(end) && start.isBefore(scheduledEnd);
    }
}
