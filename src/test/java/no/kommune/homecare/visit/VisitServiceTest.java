package no.kommune.homecare.visit;

import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.nurse.NurseService;
import no.kommune.homecare.patient.CareLevel;
import no.kommune.homecare.patient.Patient;
import no.kommune.homecare.patient.PatientService;
import no.kommune.homecare.visit.dto.VisitRequest;
import no.kommune.homecare.websocket.WebSocketEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies the core rule of feature 1: a nurse can never end up with two
 * overlapping visits, whether the overlap is introduced at creation,
 * reschedule, or reassignment time.
 */
class VisitServiceTest {

    @Mock
    private VisitRepository visitRepository;
    @Mock
    private PatientService patientService;
    @Mock
    private NurseService nurseService;
    @Mock
    private WebSocketEventPublisher eventPublisher;

    private VisitService visitService;

    private Nurse nurse;
    private Patient patient;
    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        visitService = new VisitService(visitRepository, patientService, nurseService, eventPublisher);

        nurse = withId(Nurse.builder().fullName("Kari Nordmann")
                .employeeId("N1").municipality("Oslo").active(true).build());
        patient = withId(Patient.builder().fullName("Test Patient")
                .nationalId("01019012345").municipality("Oslo").careLevel(CareLevel.MEDIUM).active(true).build());

        start = Instant.now().plus(1, ChronoUnit.DAYS);
        end = start.plus(1, ChronoUnit.HOURS);

        when(patientService.getById(patient.getId())).thenReturn(patient);
        when(nurseService.getById(nurse.getId())).thenReturn(nurse);
        when(visitRepository.save(any(Visit.class))).thenAnswer(inv -> withId(inv.getArgument(0)));
    }

    @Test
    void rejectsNewVisitThatOverlapsAnExistingVisitForTheSameNurse() {
        Visit existing = withId(Visit.builder().patient(patient).nurse(nurse)
                .scheduledStart(start).scheduledEnd(end)
                .status(VisitStatus.SCHEDULED).visitType(VisitType.MEDICATION).build());

        when(visitRepository.findByNurseIdAndScheduledStartBetween(any(), any(), any()))
                .thenReturn(List.of(existing));

        VisitRequest overlapping = new VisitRequest(
                patient.getId(), nurse.getId(),
                start.plus(30, ChronoUnit.MINUTES), end.plus(30, ChronoUnit.MINUTES),
                VisitType.MEDICATION, null, null);

        assertThatThrownBy(() -> visitService.create(overlapping))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void allowsNewVisitThatDoesNotOverlapAnyExistingVisit() {
        when(visitRepository.findByNurseIdAndScheduledStartBetween(any(), any(), any()))
                .thenReturn(List.of());

        VisitRequest nonOverlapping = new VisitRequest(
                patient.getId(), nurse.getId(), start, end, VisitType.MEDICATION, null, null);

        Visit created = visitService.create(nonOverlapping);

        assertThat(created.getStatus()).isEqualTo(VisitStatus.SCHEDULED);
        assertThat(created.getNurse()).isEqualTo(nurse);
    }

    @Test
    void ignoresCancelledVisitsWhenCheckingForConflicts() {
        Visit cancelled = withId(Visit.builder().patient(patient).nurse(nurse)
                .scheduledStart(start).scheduledEnd(end)
                .status(VisitStatus.CANCELLED).visitType(VisitType.MEDICATION).build());

        when(visitRepository.findByNurseIdAndScheduledStartBetween(any(), any(), any()))
                .thenReturn(List.of(cancelled));

        VisitRequest overlapping = new VisitRequest(
                patient.getId(), nurse.getId(), start, end, VisitType.MEDICATION, null, null);

        Visit created = visitService.create(overlapping);

        assertThat(created).isNotNull();
    }

    @Test
    void rejectsVisitWhereStartIsNotBeforeEnd() {
        VisitRequest backwards = new VisitRequest(
                patient.getId(), nurse.getId(), end, start, VisitType.MEDICATION, null, null);

        assertThatThrownBy(() -> visitService.create(backwards))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsRescheduleThatWouldOverlapAnotherVisit() {
        Visit visit = withId(Visit.builder().patient(patient).nurse(nurse)
                .scheduledStart(start).scheduledEnd(end)
                .status(VisitStatus.SCHEDULED).visitType(VisitType.MEDICATION).build());
        Visit other = withId(Visit.builder().patient(patient).nurse(nurse)
                .scheduledStart(start.plus(2, ChronoUnit.HOURS)).scheduledEnd(end.plus(2, ChronoUnit.HOURS))
                .status(VisitStatus.SCHEDULED).visitType(VisitType.MEDICATION).build());

        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(visitRepository.findByNurseIdAndScheduledStartBetween(any(), any(), any()))
                .thenReturn(List.of(other));

        Instant newStart = start.plus(2, ChronoUnit.HOURS).plus(15, ChronoUnit.MINUTES);
        Instant newEnd = end.plus(2, ChronoUnit.HOURS).plus(15, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> visitService.reschedule(visit.getId(), newStart, newEnd))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static <T extends no.kommune.homecare.common.entity.BaseEntity> T withId(T entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        return entity;
    }
}
