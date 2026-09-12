package no.kommune.homecare.absence;

import no.kommune.homecare.audit.AuditService;
import no.kommune.homecare.nurse.Nurse;
import no.kommune.homecare.nurse.NurseRepository;
import no.kommune.homecare.patient.CareLevel;
import no.kommune.homecare.patient.Patient;
import no.kommune.homecare.visit.Visit;
import no.kommune.homecare.visit.VisitRepository;
import no.kommune.homecare.visit.VisitService;
import no.kommune.homecare.visit.VisitStatus;
import no.kommune.homecare.visit.VisitType;
import no.kommune.homecare.websocket.WebSocketEventPublisher;
import no.kommune.homecare.websocket.dto.RedistributionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the core rule of feature 2: when a nurse is marked absent, every
 * visit they had scheduled during the absence window is picked up by the
 * least-loaded available substitute in the same municipality, and any visit
 * that cannot be covered is left unassigned rather than silently dropped.
 */
class VisitRedistributionServiceTest {

    @Mock
    private VisitRepository visitRepository;
    @Mock
    private NurseRepository nurseRepository;
    @Mock
    private AbsenceRepository absenceRepository;
    @Mock
    private VisitService visitService;
    @Mock
    private WebSocketEventPublisher eventPublisher;
    @Mock
    private AuditService auditService;

    private VisitRedistributionService redistributionService;

    private Nurse absentNurse;
    private Nurse lightlyLoadedNurse;
    private Nurse busyNurse;
    private Patient patient;
    private Instant visitStart;
    private Instant visitEnd;
    private Absence absence;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        redistributionService = new VisitRedistributionService(
                visitRepository, nurseRepository, absenceRepository, visitService, eventPublisher, auditService);

        absentNurse = withId(Nurse.builder().fullName("Absent Nurse")
                .employeeId("N1").municipality("Oslo").active(true).build());
        lightlyLoadedNurse = withId(Nurse.builder().fullName("Free Nurse")
                .employeeId("N2").municipality("Oslo").active(true).build());
        busyNurse = withId(Nurse.builder().fullName("Busy Nurse")
                .employeeId("N3").municipality("Oslo").active(true).build());

        patient = withId(Patient.builder().fullName("Test Patient")
                .nationalId("01019012345").municipality("Oslo").careLevel(CareLevel.MEDIUM).active(true).build());

        visitStart = Instant.now().plus(1, ChronoUnit.DAYS);
        visitEnd = visitStart.plus(1, ChronoUnit.HOURS);

        absence = withId(Absence.builder().nurse(absentNurse)
                .startDateTime(visitStart.minus(1, ChronoUnit.HOURS))
                .endDateTime(visitEnd.plus(1, ChronoUnit.HOURS))
                .reason(AbsenceReason.SICK_LEAVE)
                .redistributed(false)
                .build());
    }

    @Test
    void reassignsAffectedVisitToLeastLoadedAvailableNurse() {
        Visit affectedVisit = withId(Visit.builder().patient(patient).nurse(absentNurse)
                .scheduledStart(visitStart).scheduledEnd(visitEnd)
                .status(VisitStatus.SCHEDULED).visitType(VisitType.MEDICATION).build());

        Visit busyNurseOtherVisit = withId(Visit.builder().patient(patient).nurse(busyNurse)
                .scheduledStart(visitStart.plus(3, ChronoUnit.HOURS)).scheduledEnd(visitEnd.plus(3, ChronoUnit.HOURS))
                .status(VisitStatus.SCHEDULED).visitType(VisitType.MEDICATION).build());

        when(visitRepository.findByNurseIdAndStatusAndScheduledStartBetween(
                eq(absentNurse.getId()), eq(VisitStatus.SCHEDULED), any(), any()))
                .thenReturn(List.of(affectedVisit));

        when(nurseRepository.findByMunicipalityIgnoreCaseAndActiveTrue("Oslo"))
                .thenReturn(List.of(lightlyLoadedNurse, busyNurse));

        when(visitRepository.findByNurseIdAndScheduledStartBetween(eq(lightlyLoadedNurse.getId()), any(), any()))
                .thenReturn(List.of());
        when(visitRepository.findByNurseIdAndScheduledStartBetween(eq(busyNurse.getId()), any(), any()))
                .thenReturn(List.of(busyNurseOtherVisit));

        when(absenceRepository.findByNurseIdAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
                any(), any(), any())).thenReturn(List.of());

        RedistributionMessage result = redistributionService.redistribute(absence);

        verify(visitService).assignNurse(affectedVisit.getId(), lightlyLoadedNurse.getId());
        assertThat(result.reassignments()).hasSize(1);
        assertThat(result.reassignments().get(0).newNurseId()).isEqualTo(lightlyLoadedNurse.getId());
        assertThat(result.unassignedVisitIds()).isEmpty();
        assertThat(absence.isRedistributed()).isTrue();
    }

    @Test
    void leavesVisitUnassignedWhenNoSubstituteIsAvailable() {
        Visit affectedVisit = withId(Visit.builder().patient(patient).nurse(absentNurse)
                .scheduledStart(visitStart).scheduledEnd(visitEnd)
                .status(VisitStatus.SCHEDULED).visitType(VisitType.MEDICATION).build());

        when(visitRepository.findByNurseIdAndStatusAndScheduledStartBetween(
                eq(absentNurse.getId()), eq(VisitStatus.SCHEDULED), any(), any()))
                .thenReturn(List.of(affectedVisit));

        when(nurseRepository.findByMunicipalityIgnoreCaseAndActiveTrue("Oslo"))
                .thenReturn(List.of());

        RedistributionMessage result = redistributionService.redistribute(absence);

        verify(visitService).unassign(affectedVisit.getId());
        assertThat(result.reassignments()).isEmpty();
        assertThat(result.unassignedVisitIds()).containsExactly(affectedVisit.getId());
    }

    /** Test entities get their id via the inherited setter since {@code @Builder} does not see superclass fields. */
    private static <T extends no.kommune.homecare.common.entity.BaseEntity> T withId(T entity) {
        entity.setId(UUID.randomUUID());
        return entity;
    }
}
