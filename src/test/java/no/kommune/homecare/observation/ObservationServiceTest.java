package no.kommune.homecare.observation;

import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.nurse.NurseService;
import no.kommune.homecare.observation.dto.ObservationRequest;
import no.kommune.homecare.patient.CareLevel;
import no.kommune.homecare.patient.Patient;
import no.kommune.homecare.patient.PatientService;
import no.kommune.homecare.visit.VisitService;
import no.kommune.homecare.websocket.WebSocketEventPublisher;
import no.kommune.homecare.websocket.dto.UrgentAlertMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies feature 3's urgent-flag rule: an urgent observation always raises
 * a real-time alert, a non-urgent one never does, and a medication entry
 * without a medication name is rejected outright.
 */
class ObservationServiceTest {

    @Mock
    private ObservationRepository observationRepository;
    @Mock
    private PatientService patientService;
    @Mock
    private NurseService nurseService;
    @Mock
    private VisitService visitService;
    @Mock
    private WebSocketEventPublisher eventPublisher;

    private ObservationService observationService;

    private Patient patient;
    private Nurse nurse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        observationService = new ObservationService(
                observationRepository, patientService, nurseService, visitService, eventPublisher);

        patient = withId(Patient.builder().fullName("Test Patient")
                .nationalId("01019012345").municipality("Oslo").careLevel(CareLevel.MEDIUM).active(true).build());
        nurse = withId(Nurse.builder().fullName("Kari Nordmann")
                .employeeId("N1").municipality("Oslo").active(true).build());

        when(patientService.getById(patient.getId())).thenReturn(patient);
        when(nurseService.getById(nurse.getId())).thenReturn(nurse);
        when(observationRepository.save(any(Observation.class))).thenAnswer(inv -> withId(inv.getArgument(0)));
    }

    @Test
    void publishesUrgentAlertWhenObservationIsFlaggedUrgent() {
        ObservationRequest request = new ObservationRequest(
                patient.getId(), nurse.getId(), null, ObservationType.INCIDENT,
                null, null, null, null, "Patient fell in the bathroom", true);

        observationService.create(request);

        verify(eventPublisher, times(1)).publishUrgentAlert(any(UrgentAlertMessage.class));
    }

    @Test
    void doesNotPublishUrgentAlertWhenObservationIsNotFlagged() {
        ObservationRequest request = new ObservationRequest(
                patient.getId(), nurse.getId(), null, ObservationType.MOOD,
                null, null, null, MoodLevel.GOOD, "Patient in good spirits today", false);

        observationService.create(request);

        verify(eventPublisher, never()).publishUrgentAlert(any());
    }

    @Test
    void rejectsMedicationObservationWithoutMedicationName() {
        ObservationRequest request = new ObservationRequest(
                patient.getId(), nurse.getId(), null, ObservationType.MEDICATION,
                null, "10mg", true, null, "Gave medication", false);

        assertThatThrownBy(() -> observationService.create(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsResolvingUrgentFlagOnAnObservationThatIsNotUrgent() {
        Observation observation = withId(Observation.builder()
                .patient(patient).recordedBy(nurse)
                .observationType(ObservationType.MOOD)
                .description("Fine")
                .urgent(false)
                .urgentResolved(false)
                .build());

        when(observationRepository.findById(observation.getId())).thenReturn(java.util.Optional.of(observation));

        assertThatThrownBy(() -> observationService.resolveUrgent(observation.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static <T extends no.kommune.homecare.common.entity.BaseEntity> T withId(T entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        return entity;
    }
}
