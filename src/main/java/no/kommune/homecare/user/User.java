package no.kommune.homecare.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.kommune.homecare.common.entity.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "app_user", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = "username"),
        @jakarta.persistence.UniqueConstraint(columnNames = "email")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class User extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 150)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * The kommune this account is scoped to. Null marks a platform-wide
     * account (not tied to a single municipality); every other account is
     * confined by {@link no.kommune.homecare.security.CurrentUser} to only
     * ever read or write resources in this municipality, regardless of role.
     */
    @Column(length = 100)
    private String municipality;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;
}
