package no.kommune.homecare;

import no.kommune.homecare.absence.Absence;
import no.kommune.homecare.absence.AbsenceReason;
import no.kommune.homecare.absence.AbsenceRepository;
import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.nurse.NurseRepository;
import no.kommune.homecare.observation.Observation;
import no.kommune.homecare.observation.ObservationRepository;
import no.kommune.homecare.observation.ObservationType;
import no.kommune.homecare.patient.CareLevel;
import no.kommune.homecare.patient.Patient;
import no.kommune.homecare.patient.PatientRepository;
import no.kommune.homecare.visit.Visit;
import no.kommune.homecare.visit.VisitRepository;
import no.kommune.homecare.visit.VisitStatus;
import no.kommune.homecare.visit.VisitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With {@code spring.jpa.open-in-view: false} (set for the main/docker
 * profiles, and mirrored in test config so this test can catch it), the
 * Hibernate session behind a lazy {@code @ManyToOne} is already closed by
 * the time a controller builds its response DTO - a real request runs the
 * service's {@code @Transactional} method, closing the session, before the
 * controller ever calls {@code XxxResponse.from(...)}. A plain
 * {@code @Transactional} test would mask this, since it keeps one session
 * open across both steps; this test deliberately runs without one, seeding
 * data through repository calls that each commit on their own, so the GET
 * requests below hit entities with genuinely uninitialized lazy proxies -
 * exactly like a live deployment.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenInViewRegressionTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private NurseRepository nurseRepository;
    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private AbsenceRepository absenceRepository;
    @Autowired
    private ObservationRepository observationRepository;

    private Patient patient;
    private Nurse nurse;

    @BeforeEach
    void setUp() {
        // Each test commits its own data (no test-level @Transactional - see class
        // Javadoc) and shares one H2 database across the whole class, so identifiers
        // subject to a unique constraint must not repeat between test methods.
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String nationalId = String.format("%011d",
                Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 100_000_000_000L);
        patient = patientRepository.save(Patient.builder()
                .fullName("Ola Hansen").nationalId(nationalId)
                .municipality("Oslo").careLevel(CareLevel.MEDIUM).active(true).build());
        nurse = nurseRepository.save(Nurse.builder()
                .fullName("Kari Nordmann").employeeId("N-REG-" + unique)
                .municipality("Oslo").active(true).build());
    }

    @Test
    @WithMockUser
    void visitsByDateIncludeNurseAndPatientNames() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        visitRepository.save(Visit.builder()
                .patient(patient).nurse(nurse)
                .scheduledStart(start).scheduledEnd(start.plus(30, ChronoUnit.MINUTES))
                .status(VisitStatus.SCHEDULED).visitType(VisitType.MEDICATION).build());

        mockMvc.perform(get("/api/visits").param("date", start.toString().substring(0, 10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientName").value("Ola Hansen"))
                .andExpect(jsonPath("$[0].nurseName").value("Kari Nordmann"));
    }

    @Test
    @WithMockUser
    void absenceListIncludesNurseName() throws Exception {
        absenceRepository.save(Absence.builder()
                .nurse(nurse)
                .startDateTime(Instant.now())
                .endDateTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .reason(AbsenceReason.SICK_LEAVE)
                .redistributed(false)
                .build());

        mockMvc.perform(get("/api/absences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nurseName").value("Kari Nordmann"));
    }

    @Test
    @WithMockUser
    void urgentObservationListIncludesPatientAndNurseNames() throws Exception {
        observationRepository.save(Observation.builder()
                .patient(patient).recordedBy(nurse)
                .observationType(ObservationType.INCIDENT)
                .description("Fall in bathroom")
                .urgent(true).urgentResolved(false)
                .recordedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/observations/urgent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientName").value("Ola Hansen"))
                .andExpect(jsonPath("$[0].recordedByNurseName").value("Kari Nordmann"));
    }
}
