package no.kommune.homecare.security;

import lombok.RequiredArgsConstructor;
import no.kommune.homecare.audit.AuditAction;
import no.kommune.homecare.audit.AuditService;
import no.kommune.homecare.common.exception.BusinessRuleException;
import no.kommune.homecare.security.dto.LoginRequest;
import no.kommune.homecare.security.dto.LoginResponse;
import no.kommune.homecare.security.dto.RegisterRequest;
import no.kommune.homecare.user.Role;
import no.kommune.homecare.user.User;
import no.kommune.homecare.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessRuleException("Invalid username or password"));

        AuthenticatedUser principal = AuthenticatedUser.of(user);
        String token = jwtService.generateToken(principal, user.getRole().name());

        auditService.record(AuditAction.LOGIN, "User", user.getId(), "User logged in");

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getFullName(), user.getRole().name());
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessRuleException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Email already registered: " + request.email());
        }
        if (request.role() != Role.ADMIN && (request.municipality() == null || request.municipality().isBlank())) {
            throw new BusinessRuleException("municipality is required for role " + request.role());
        }
        CurrentUser.assertAccessible(request.municipality());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .municipality(request.municipality())
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        auditService.record(AuditAction.CREATE, "User", saved.getId(), "User account created: " + saved.getUsername());
        return saved;
    }
}
