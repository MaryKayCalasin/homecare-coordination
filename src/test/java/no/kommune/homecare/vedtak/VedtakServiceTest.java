package no.kommune.homecare.vedtak;

import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.patient.CareLevel;
import no.kommune.homecare.patient.Patient;
import no.kommune.homecare.patient.PatientService;
import no.kommune.homecare.vedtak.dto.VedtakRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Covers the foundation Vedtak provides: a decision is created against a
 * patient the caller can access, with a sane validity window, and can later
 * be revoked without being deleted (the decision history itself matters).
 */
class VedtakServiceTest {

    @Mock
    private VedtakRepository vedtakRepository;
    @Mock
    private PatientService patientService;

    private VedtakService vedtakService;
    private Patient patient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        vedtakService = new VedtakService(vedtakRepository, patientService);

        patient = Patient.builder().fullName("Test Patient")
                .nationalId("01019012345").municipality("Oslo").careLevel(CareLevel.MEDIUM).active(true).build();
        patient.setId(UUID.randomUUID());

        when(patientService.getById(patient.getId())).thenReturn(patient);
        when(vedtakRepository.save(any(Vedtak.class))).thenAnswer(inv -> {
            Vedtak v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
    }

    @Test
    void createsAnActiveVedtakForAnAccessiblePatient() {
        VedtakRequest request = new VedtakRequest(
                patient.getId(), IplosServiceType.HOME_NURSING, new BigDecimal("5.0"),
                LocalDate.now(), null, "Saksbehandler Hansen", null);

        Vedtak created = vedtakService.create(request);

        assertThat(created.getStatus()).isEqualTo(VedtakStatus.ACTIVE);
        assertThat(created.getPatient()).isEqualTo(patient);
        assertThat(created.getServiceType()).isEqualTo(IplosServiceType.HOME_NURSING);
    }

    @Test
    void rejectsAValidToBeforeValidFrom() {
        VedtakRequest request = new VedtakRequest(
                patient.getId(), IplosServiceType.HOME_NURSING, new BigDecimal("5.0"),
                LocalDate.now(), LocalDate.now().minusDays(1), "Saksbehandler Hansen", null);

        assertThatThrownBy(() -> vedtakService.create(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void revokeChangesStatusWithoutDeletingTheRecord() {
        Vedtak vedtak = Vedtak.builder().patient(patient).serviceType(IplosServiceType.HOME_NURSING)
                .grantedHoursPerWeek(new BigDecimal("5.0")).validFrom(LocalDate.now())
                .decidedBy("Saksbehandler Hansen").decidedAt(java.time.Instant.now())
                .status(VedtakStatus.ACTIVE).build();
        vedtak.setId(UUID.randomUUID());
        when(vedtakRepository.findById(vedtak.getId())).thenReturn(Optional.of(vedtak));

        Vedtak revoked = vedtakService.revoke(vedtak.getId());

        assertThat(revoked.getStatus()).isEqualTo(VedtakStatus.REVOKED);
        assertThat(revoked.getId()).isEqualTo(vedtak.getId());
    }
}
