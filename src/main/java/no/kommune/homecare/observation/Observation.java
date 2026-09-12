package no.kommune.homecare.observation;

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
import no.kommune.homecare.visit.Visit;

import java.time.Instant;

/**
 * A single journal entry recorded by a nurse about a patient: medication
 * administration, mood, vital signs, or any other observation made during
 * or between visits. Entries flagged {@code urgent} surface immediately to
 * coordinators over WebSocket and are highlighted in the shift handover
 * report until resolved.
 */
@Getter
@Setter
@Entity
@Table(name = "observation", indexes = {
        @Index(name = "idx_observation_patient_recorded", columnList = "patient_id,recordedAt"),
        @Index(name = "idx_observation_urgent", columnList = "urgent,urgentResolved")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Observation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_nurse_id", nullable = false)
    private Nurse recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObservationType observationType;

    @Column(length = 200)
    private String medicationName;

    @Column(length = 100)
    private String medicationDosage;

    private Boolean medicationGiven;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MoodLevel moodLevel;

    @Column(nullable = false, length = 2000)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean urgent = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean urgentResolved = false;

    private Instant urgentResolvedAt;

    @Column(length = 100)
    private String urgentResolvedBy;

    @Column(nullable = false)
    private Instant recordedAt;
}
