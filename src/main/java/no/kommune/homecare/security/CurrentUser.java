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

    /** The current account's bydel, or empty if unset - true for every kommune except Oslo. */
    public static Optional<String> bydel() {
        return get().map(AuthenticatedUser::getBydel).filter(b -> b != null && !b.isBlank());
    }

    /**
     * Throws {@link AccessDeniedException} if the current account is
     * municipality-scoped and {@code resourceMunicipality} doesn't match.
     * A platform-wide account (or no authentication at all) always passes.
     */
    public static void assertAccessible(String resourceMunicipality) {
        assertAccessible(resourceMunicipality, null);
    }

    /**
     * As {@link #assertAccessible(String)}, plus - only when the current
     * account itself has a bydel set - requires {@code resourceBydel} to
     * match it too. A resource with no bydel recorded fails this check
     * rather than being silently let through: for a bydel-scoped Oslo
     * account, an untagged patient is a data-quality gap to fix, not an
     * implicit grant to every bydel's staff.
     */
    public static void assertAccessible(String resourceMunicipality, String resourceBydel) {
        get().ifPresent(user -> {
            String ownMunicipality = user.getMunicipality();
            if (ownMunicipality == null || ownMunicipality.isBlank()) {
                return; // platform-wide account
            }
            if (resourceMunicipality == null || !resourceMunicipality.equalsIgnoreCase(ownMunicipality)) {
                throw new AccessDeniedException("Not authorized for this municipality");
            }
            String ownBydel = user.getBydel();
            if (ownBydel != null && !ownBydel.isBlank()
                    && (resourceBydel == null || !resourceBydel.equalsIgnoreCase(ownBydel))) {
                throw new AccessDeniedException("Not authorized for this bydel");
            }
        });
    }
}
