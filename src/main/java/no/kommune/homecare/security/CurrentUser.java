package no.kommune.homecare.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Enforces kommune tenant isolation: a logged-in account may only read or
 * write resources belonging to its own municipality, regardless of role.
 * A {@code null} municipality on the authenticated principal marks a
 * platform-wide account and is never scoped.
 * <p>
 * Reads the principal straight from {@link SecurityContextHolder} rather
 * than being threaded through every controller and service method. When no
 * authentication is present - true only outside a real request, such as a
 * unit test that calls a service directly - every check is a no-op, since
 * {@code SecurityConfig} already guarantees {@code anyRequest().authenticated()}
 * for every real request before controller code runs.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    /** The current account's municipality, or empty if unauthenticated or platform-wide. */
    public static Optional<String> municipality() {
        return get().map(AuthenticatedUser::getMunicipality).filter(m -> m != null && !m.isBlank());
    }

    /**
     * Throws {@link AccessDeniedException} if the current account is
     * municipality-scoped and {@code resourceMunicipality} doesn't match.
     * A platform-wide account (or no authentication at all) always passes.
     */
    public static void assertAccessible(String resourceMunicipality) {
        municipality().ifPresent(own -> {
            if (resourceMunicipality == null || !resourceMunicipality.equalsIgnoreCase(own)) {
                throw new AccessDeniedException("Not authorized for this municipality");
            }
        });
    }
}
