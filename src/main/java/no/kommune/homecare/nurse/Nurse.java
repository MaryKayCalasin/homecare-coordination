package no.kommune.homecare.nurse;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.kommune.homecare.common.entity.BaseEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "nurse")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Nurse extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 30)
    private String employeeId;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, length = 100)
    private String municipality;

    /** Links to the login account this nurse uses, if they have platform access. */
    @Column
    private UUID userId;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "nurse_qualification", joinColumns = @JoinColumn(name = "nurse_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "qualification", length = 40)
    private Set<Qualification> qualifications = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

// Work Capacity
    @Column (nullable = false)
    private boolean canGiveMedication = false;

    @Column(nullable = false)
    private boolean canDoWoundCare = false;

    @Column(nullable = false)
    private boolean canDoIV = false;

    @Column(nullable = false)
    private boolean canLiftHeavy = false;

    @Column(nullable = false)
    private boolean canWorkAlone = false;

    @Column(nullable = false)
    private boolean canHandleDementia = false;

    @Column(nullable = false)
    private boolean canDoPersonalCare = true;

}
