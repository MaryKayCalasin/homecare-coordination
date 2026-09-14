package no.kommune.homecare.security;

import lombok.Getter;
import no.kommune.homecare.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

@Getter
public class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String password;
    private final String fullName;
    private final String role;
    private final String municipality;
    private final String bydel;
    private final boolean enabled;

    private AuthenticatedUser(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.role = user.getRole().name();
        this.municipality = user.getMunicipality();
        this.bydel = user.getBydel();
        this.enabled = user.isEnabled();
    }

    public static AuthenticatedUser of(User user) {
        return new AuthenticatedUser(user);
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
