package no.kommune.homecare.security;

import no.kommune.homecare.audit.AuditService;
import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.security.dto.LoginRequest;
import no.kommune.homecare.security.dto.LoginResponse;
import no.kommune.homecare.security.dto.RegisterRequest;
import no.kommune.homecare.user.Role;
import no.kommune.homecare.user.User;
import no.kommune.homecare.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the account rules referenced in the README: login issues a JWT
 * and records an audit entry, and registration rejects a username or email
 * that's already taken rather than silently overwriting it.
 */
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditService auditService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(authenticationManager, userRepository, passwordEncoder, jwtService, auditService);
    }

    @Test
    void loginReturnsTokenAndRecordsAuditEntry() {
        User user = User.builder().username("kari.nordmann").email("kari@example.no")
                .passwordHash("hashed").fullName("Kari Nordmann").role(Role.NURSE).enabled(true).build();
        user.setId(UUID.randomUUID());

        when(userRepository.findByUsername("kari.nordmann")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(), anyString())).thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(new LoginRequest("kari.nordmann", "ChangeMe123!"));

        assertThat(response.token()).isEqualTo("fake-jwt-token");
        assertThat(response.username()).isEqualTo("kari.nordmann");
        assertThat(response.role()).isEqualTo("NURSE");
        verify(auditService).record(any(), any(), any(), anyString());
    }

    @Test
    void rejectsRegistrationWithAlreadyTakenUsername() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("admin", "new@example.no", "ChangeMe123!", "New Admin", Role.ADMIN, null);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsRegistrationWithAlreadyRegisteredEmail() {
        when(userRepository.existsByUsername("new.nurse")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.no")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("new.nurse", "taken@example.no", "ChangeMe123!", "New Nurse", Role.NURSE, "Oslo");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsNonAdminRegistrationWithoutMunicipality() {
        when(userRepository.existsByUsername("new.nurse")).thenReturn(false);
        when(userRepository.existsByEmail("new.nurse@example.no")).thenReturn(false);

        RegisterRequest request = new RegisterRequest(
                "new.nurse", "new.nurse@example.no", "ChangeMe123!", "New Nurse", Role.NURSE, " ");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("municipality");
    }
}
