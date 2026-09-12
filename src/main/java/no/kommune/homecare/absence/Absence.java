package no.kommune.homecare.absence;

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
import no.kommune.homecare.nurse.Nurse;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "absence")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Absence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nurse_id", nullable = false)
    private Nurse nurse;

    @Column(nullable = false)
    private Instant startDateTime;

    @Column(nullable = false)
    private Instant endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AbsenceReason reason;

    @Column(length = 500)
    private String notes;

    @Builder.Default
    @Column(nullable = false)
    private boolean redistributed = false;
}
