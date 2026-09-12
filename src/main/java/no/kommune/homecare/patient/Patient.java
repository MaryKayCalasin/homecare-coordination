package no.kommune.homecare.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * A person receiving home care ("hjemmesykepleie" / "hjemmetjeneste") from
 * the municipality. Contains sensitive personal and health data and must
 * only ever be accessed through audited service methods.
 */
@Getter
@Setter
@Entity
@Table(name = "patient")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Patient extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String fullName;

    /** Norwegian national identity number (fødselsnummer), 11 digits. */
    @Column(nullable = false, unique = true, length = 11)
    private String nationalId;

    private LocalDate dateOfBirth;

    @Column(length = 200)
    private String address;

    @Column(length = 10)
    private String postalCode;

    @Column(length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String municipality;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String nextOfKinName;

    @Column(length = 20)
    private String nextOfKinPhone;

    @Column(length = 500)
    private String primaryDiagnosis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareLevel careLevel;

    @Column(length = 2000)
    private String careNotes;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
