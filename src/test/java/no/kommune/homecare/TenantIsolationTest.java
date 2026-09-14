package no.kommune.homecare;

import no.kommune.homecare.patient.CareLevel;
import no.kommune.homecare.patient.Patient;
import no.kommune.homecare.patient.PatientRepository;
import no.kommune.homecare.security.AuthenticatedUser;
import no.kommune.homecare.user.Role;
import no.kommune.homecare.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards against the cross-tenant leak this project shipped with: before
 * {@link no.kommune.homecare.security.CurrentUser} existed, {@code GET
 * /api/patients/{id}} checked only role, never that the caller's own
 * municipality matched the patient's - any COORDINATOR account could fetch
 * any patient in any kommune by ID. This exercises the real authenticated
 * principal type ({@link AuthenticatedUser}), not {@code @WithMockUser}'s
 * generic one, since {@code CurrentUser} specifically looks for it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;
    @Autowired
    private PatientRepository patientRepository;

    private Patient osloPatient;
    private Patient bergenPatient;
    private Patient sagenePatient;
    private Patient grunerlokkaPatient;
    private Authentication osloCoordinator;
    private Authentication sageneCoordinator;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        osloPatient = patientRepository.save(Patient.builder()
                .fullName("Ola Hansen").nationalId(nationalId())
                .municipality("Oslo").careLevel(CareLevel.MEDIUM).active(true).build());
        bergenPatient = patientRepository.save(Patient.builder()
                .fullName("Kari Bergen").nationalId(nationalId())
                .municipality("Bergen").careLevel(CareLevel.MEDIUM).active(true).build());
        sagenePatient = patientRepository.save(Patient.builder()
                .fullName("Sagene Patient").nationalId(nationalId())
                .municipality("Oslo").bydel("Sagene").careLevel(CareLevel.MEDIUM).active(true).build());
        grunerlokkaPatient = patientRepository.save(Patient.builder()
                .fullName("Grünerløkka Patient").nationalId(nationalId())
                .municipality("Oslo").bydel("Grünerløkka").careLevel(CareLevel.MEDIUM).active(true).build());

        User user = User.builder()
                .username("coord.oslo." + unique).email("coord.oslo." + unique + "@example.no")
                .passwordHash("irrelevant").fullName("Oslo Coordinator")
                .role(Role.COORDINATOR).municipality("Oslo").enabled(true).build();
        user.setId(UUID.randomUUID());
        AuthenticatedUser principal = AuthenticatedUser.of(user);
        osloCoordinator = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        User sageneUser = User.builder()
                .username("coord.sagene." + unique).email("coord.sagene." + unique + "@example.no")
                .passwordHash("irrelevant").fullName("Sagene Coordinator")
                .role(Role.COORDINATOR).municipality("Oslo").bydel("Sagene").enabled(true).build();
        sageneUser.setId(UUID.randomUUID());
        AuthenticatedUser sagenePrincipal = AuthenticatedUser.of(sageneUser);
        sageneCoordinator = new UsernamePasswordAuthenticationToken(sagenePrincipal, null, sagenePrincipal.getAuthorities());
    }

    @Test
    void canReadAPatientInOwnMunicipality() throws Exception {
        mockMvc.perform(get("/api/patients/{id}", osloPatient.getId()).with(authentication(osloCoordinator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ola Hansen"));
    }

    @Test
    void cannotReadAPatientInAnotherMunicipality() throws Exception {
        mockMvc.perform(get("/api/patients/{id}", bergenPatient.getId()).with(authentication(osloCoordinator)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listingPatientsOnlyReturnsOwnMunicipality() throws Exception {
        mockMvc.perform(get("/api/patients").with(authentication(osloCoordinator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].municipality").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("Oslo"))));
    }

    @Test
    void canReadAPatientInOwnBydel() throws Exception {
        mockMvc.perform(get("/api/patients/{id}", sagenePatient.getId()).with(authentication(sageneCoordinator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Sagene Patient"));
    }

    @Test
    void cannotReadAPatientInAnotherBydelOfTheSameKommune() throws Exception {
        // Same municipality (Oslo) as the caller - only bydel differs. The old
        // municipality-only check would have let this through.
        mockMvc.perform(get("/api/patients/{id}", grunerlokkaPatient.getId()).with(authentication(sageneCoordinator)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aMunicipalityScopedCoordinatorWithNoBydelCanStillSeeBothBydeler() throws Exception {
        // No bydel set on this principal, so only the municipality check applies -
        // this is the platform-wide-within-Oslo case (e.g. an Oslo-level admin).
        mockMvc.perform(get("/api/patients/{id}", sagenePatient.getId()).with(authentication(osloCoordinator)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/{id}", grunerlokkaPatient.getId()).with(authentication(osloCoordinator)))
                .andExpect(status().isOk());
    }

    private static String nationalId() {
        return String.format("%011d", Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 100_000_000_000L);
    }
}
