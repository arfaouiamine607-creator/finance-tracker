package com.financetracker.config;

// Our JWT filter that runs before every request
import com.financetracker.security.JwtAuthFilter;

// Our UserRepository — to look up users by email when loading them for auth
import com.financetracker.repository.UserRepository;

// Spring Security imports for configuring authentication and HTTP rules
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// This class configures all the security rules for our app.
// It answers three questions:
//   1. Which routes can anyone access? (register, login)
//   2. Which routes need a valid JWT token? (everything else)
//   3. How does Spring Security load and verify users?
@Configuration      // tells Spring this class contains configuration (bean definitions)
@EnableWebSecurity  // turns on Spring Security for the whole application
public class SecurityConfig {

    // Our filter that reads and validates the JWT token on every request
    private final JwtAuthFilter jwtAuthFilter;

    // Gives us access to the users table so we can load a user by their email
    private final UserRepository userRepository;

    // Spring injects both dependencies through the constructor
    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserRepository userRepository) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userRepository = userRepository;
    }

    // Defines how Spring Security loads a user from the database by their email.
    // We write it as a lambda (one-liner function) — no need for a separate class.
    // Spring Security calls this whenever it needs to look up who a user is.
    @Bean
    public UserDetailsService userDetailsService() {
        // "email" is the value Spring passes in — it comes from the JWT token or login form
        return email -> userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
    }

    // This bean defines the HTTP security rules — the heart of our config.
    // It's a chain of rules Spring checks for every incoming request.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (Cross-Site Request Forgery) protection.
            // CSRF is needed for browser-based form logins (HTML forms with sessions).
            // We use JWT tokens instead of sessions, so CSRF is not relevant here.
            .csrf(csrf -> csrf.disable())

            // Define which routes are public and which require a token
            .authorizeHttpRequests(auth -> auth
                // Anyone can call /api/auth/register and /api/auth/login
                // without a token — this is how new users sign up and get their token
                .requestMatchers("/auth/**").permitAll()
                // Allow Spring's internal /error endpoint so that error responses
                // (400 validation errors, 500 server errors) reach the client correctly.
                // Without this, Spring Security blocks the error forward and returns 403
                // instead of the real error code.
                .requestMatchers("/error").permitAll()
                // Every other route requires the user to be authenticated (have a valid token)
                .anyRequest().authenticated()
            )

            // Use stateless sessions — Spring will NOT create an HTTP session for users.
            // Instead, every request must carry its own JWT token.
            // This is the standard approach for REST APIs.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Tell Spring Security to use our custom authentication provider
            // (the one that loads users from the database and checks bcrypt passwords)
            .authenticationProvider(authenticationProvider())

            // Insert our JWT filter BEFORE Spring's default login filter.
            // This means our filter runs first: it reads the token and sets the user
            // in the security context, then Spring's filter sees the user is already
            // authenticated and doesn't ask for a username/password again.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build(); // finalize and return the configured security chain
    }

    // Defines HOW Spring Security checks if a username/password is correct.
    // DaoAuthenticationProvider = loads user from database, then compares password hashes.
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        // Tell it to use our userDetailsService bean to find users by email
        provider.setUserDetailsService(userDetailsService());

        // Tell it to use BCrypt when comparing the entered password to the stored hash
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    // The AuthenticationManager is the main entry point for authenticating a user.
    // We expose it as a bean so AuthService can call it during login.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
        throws Exception {
        return config.getAuthenticationManager();
    }

    // BCryptPasswordEncoder handles hashing passwords before storing them,
    // and checking a plain-text password against a stored hash during login.
    // BCrypt is the industry standard for password hashing — it's slow by design
    // so that brute-force attacks take too long to be practical.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
