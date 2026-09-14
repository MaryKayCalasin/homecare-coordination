package no.kommune.homecare.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.nurse.NurseRepository;
import no.kommune.homecare.nurse.Qualification;
import no.kommune.homecare.patient.CareLevel;
import no.kommune.homecare.patient.Patient;
import no.kommune.homecare.patient.PatientRepository;
import no.kommune.homecare.user.Role;
import no.kommune.homecare.user.User;
import no.kommune.homecare.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Seeds a minimal, obviously-fake dataset so the platform can be logged into
 * and exercised end to end without manual setup. Gated by the
 * {@code app.seed.enabled} property (not by profile name) so it can be
 * turned on for any environment that wants it - the dev profile and the
 * Docker Compose quick-start both enable it - and stays off by default
 * everywhere else, including production.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final NurseRepository nurseRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Data already present, skipping dev seed");
            return;
        }

        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@example-kommune.no")
                .passwordHash(passwordEncoder.encode("ChangeMe123!"))
                .fullName("Platform Administrator")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        // Oslo delivers hjemmetjenesten through bydeler; two are seeded here -
        // Sagene and Grünerløkka - specifically so the bydel-level tenant
        // isolation (CurrentUser) has something real to demonstrate: a Sagene
        // account should never see Grünerløkka's coordinator, nurse, or patient.
        userRepository.save(User.builder()
                .username("mari.sagene")
                .email("mari.sagene@example-kommune.no")
                .passwordHash(passwordEncoder.encode("ChangeMe123!"))
                .fullName("Mari Halvorsen")
                .role(Role.COORDINATOR)
                .municipality("Oslo")
                .bydel("Sagene")
                .enabled(true)
                .build());

        User nurseUser = userRepository.save(User.builder()
                .username("kari.nordmann")
                .email("kari.nordmann@example-kommune.no")
                .passwordHash(passwordEncoder.encode("ChangeMe123!"))
                .fullName("Kari Nordmann")
                .role(Role.NURSE)
                .municipality("Oslo")
                .bydel("Sagene")
                .enabled(true)
                .build());

        Nurse nurse = nurseRepository.save(Nurse.builder()
                .fullName("Kari Nordmann")
                .employeeId("EMP-1001")
                .phone("+47 900 00 001")
                .email(nurseUser.getEmail())
                .municipality("Oslo")
                .bydel("Sagene")
                .userId(nurseUser.getId())
                .qualifications(Set.of(Qualification.REGISTERED_NURSE, Qualification.MEDICATION_ADMINISTRATION))
                .active(true)
                .build());

        patientRepository.save(Patient.builder()
                .fullName("Ola Hansen")
                .nationalId("01019012345")
                .address("Storgata 1")
                .postalCode("0155")
                .city("Oslo")
                .municipality("Oslo")
                .bydel("Sagene")
                .phone("+47 900 00 100")
                .primaryDiagnosis("Type 2 diabetes")
                .careLevel(CareLevel.MEDIUM)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .username("per.grunerlokka")
                .email("per.grunerlokka@example-kommune.no")
                .passwordHash(passwordEncoder.encode("ChangeMe123!"))
                .fullName("Per Johansen")
                .role(Role.COORDINATOR)
                .municipality("Oslo")
                .bydel("Grünerløkka")
                .enabled(true)
                .build());

        User otherNurseUser = userRepository.save(User.builder()
                .username("lise.haugen")
                .email("lise.haugen@example-kommune.no")
                .passwordHash(passwordEncoder.encode("ChangeMe123!"))
                .fullName("Lise Haugen")
                .role(Role.NURSE)
                .municipality("Oslo")
                .bydel("Grünerløkka")
                .enabled(true)
                .build());

        nurseRepository.save(Nurse.builder()
                .fullName("Lise Haugen")
                .employeeId("EMP-1002")
                .phone("+47 900 00 002")
                .email(otherNurseUser.getEmail())
                .municipality("Oslo")
                .bydel("Grünerløkka")
                .userId(otherNurseUser.getId())
                .qualifications(Set.of(Qualification.REGISTERED_NURSE))
                .active(true)
                .build());

        patientRepository.save(Patient.builder()
                .fullName("Grete Iversen")
                .nationalId("02029012345")
                .address("Thorvald Meyers gate 10")
                .postalCode("0555")
                .city("Oslo")
                .municipality("Oslo")
                .bydel("Grünerløkka")
                .phone("+47 900 00 200")
                .primaryDiagnosis("Hoftebrudd, rehabilitering")
                .careLevel(CareLevel.HIGH)
                .active(true)
                .build());

        log.info("Seeded dev data: admin user 'admin', bydel-scoped users 'mari.sagene'/'kari.nordmann' (Sagene) "
                + "and 'per.grunerlokka'/'lise.haugen' (Grünerløkka), nurse id {}", nurse.getId());
    }
}
