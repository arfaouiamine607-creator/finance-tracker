package com.financetracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Maps to the "users" table in my PostgreSQL database.
 * I decided to use email as the login identifier instead of username —
 * it's more common in modern apps and avoids "username taken" frustration.
 *
 * I'm implementing UserDetails directly on this class so Spring Security
 * can use it for authentication without needing a separate adapter.
 *
 * Lombok's @Data generates all the getters/setters/equals/hashCode for me,
 * @Builder lets me construct users cleanly, and @NoArgsConstructor is
 * required by JPA to instantiate entities via reflection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // "user" is a reserved word in SQL so I named it "users"
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false) // stored as a bcrypt hash, never plain text
    private String password;

    // The currency the user wants all their expenses displayed in.
    // Defaults to CAD since I'm at UQAM in Montreal.
    // When a user logs an expense in MAD or USD, I convert it to this currency.
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency = "CAD";

    // I only need USER and ADMIN for now.
    // Storing as STRING ("USER"/"ADMIN") instead of ordinal (0/1)
    // so it's readable directly in the database.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Auto-sets createdAt to the current time when a new user is first saved.
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Role {
        USER,
        ADMIN
    }

    // Spring Security calls getAuthorities() to check what this user is allowed to do.
    // I prefix the role name with "ROLE_" because that's what Spring Security expects
    // for @PreAuthorize("hasRole('ADMIN')") checks to work in controllers.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    // Spring Security uses getUsername() as the unique login key.
    // I return the email here since that's what I use to log in.
    @Override
    public String getUsername() {
        return email;
    }

    // I'm keeping all accounts active for now.
    // I can add isLocked or isEnabled columns later if I need account suspension.
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
