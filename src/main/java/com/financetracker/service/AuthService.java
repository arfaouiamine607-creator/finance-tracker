package com.financetracker.service;

// The DTOs — what comes in and what goes out
import com.financetracker.dto.AuthResponse;
import com.financetracker.dto.LoginRequest;
import com.financetracker.dto.RegisterRequest;

// The User entity and its role enum
import com.financetracker.entity.User;

// The database access layer for users
import com.financetracker.repository.UserRepository;

// The class that creates JWT tokens
import com.financetracker.security.JwtUtil;

// Spring Security's tool for verifying login credentials (email + password)
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

// Spring's BCrypt password hasher
import org.springframework.security.crypto.password.PasswordEncoder;

// Marks this as a Spring service (business logic layer)
import org.springframework.stereotype.Service;

// AuthService handles two things:
//   1. register() — creates a new user account and returns a token
//   2. login()    — checks credentials and returns a token if correct
//
// The controller calls these methods and returns their result directly.
// All the real logic (database checks, password hashing, token generation) lives here.
@Service // tells Spring to create one instance of this class and make it injectable
public class AuthService {

    // Saves and looks up users in the database
    private final UserRepository userRepository;

    // Hashes passwords before saving, and checks them during login
    private final PasswordEncoder passwordEncoder;

    // Creates the JWT token after a successful register or login
    private final JwtUtil jwtUtil;

    // Verifies that an email + password combination is correct during login
    private final AuthenticationManager authenticationManager;

    // Spring injects all 4 dependencies through the constructor
    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil,
        AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // Creates a new user account.
    // Steps:
    //   1. Make sure the email isn't already registered
    //   2. Make sure the username isn't already taken
    //   3. Hash the password
    //   4. Save the user to the database
    //   5. Generate a JWT token and return it
    public AuthResponse register(RegisterRequest request) {

        // Reject registration if this email is already in the database.
        // We check before trying to save to give a clear error message
        // instead of a confusing database constraint violation.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("An account with this email already exists");
        }

        // Reject registration if this username is already taken
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("This username is already taken");
        }

        // Build the new User object.
        // We hash the password with BCrypt — the hash is what gets saved,
        // never the plain text the user typed.
        User user = User.builder()
            .username(request.getUsername())                           // display name
            .email(request.getEmail())                                 // login identifier
            .password(passwordEncoder.encode(request.getPassword()))   // hashed password
            .baseCurrency(request.getBaseCurrency())                   // e.g. "CAD"
            .role(User.Role.USER)                                      // all new accounts start as USER
            .build();

        // Save the user to the database — this triggers @PrePersist and sets createdAt
        userRepository.save(user);

        // Generate a JWT token using the user's email as the identifier
        String token = jwtUtil.generateToken(user.getEmail());

        // Return the token and user info — the frontend will store this token
        // and send it with every future request
        return AuthResponse.builder()
            .token(token)
            .username(user.getDisplayName())  // the display name (not the email)
            .email(user.getEmail())
            .baseCurrency(user.getBaseCurrency())
            .build();
    }

    // Logs in an existing user.
    // Steps:
    //   1. Use Spring Security's AuthenticationManager to verify email + password
    //   2. If wrong → it throws an exception automatically (we don't handle it here)
    //   3. If correct → load the user and generate a token
    public AuthResponse login(LoginRequest request) {

        // This one line does everything: looks up the user by email,
        // hashes the password the user typed, compares it to the stored hash.
        // If the email doesn't exist or the password is wrong, it throws
        // BadCredentialsException automatically — we don't need to check manually.
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),   // Spring Security uses email as the username
                request.getPassword() // the raw password the user typed
            )
        );

        // If we reach this line, the credentials were correct.
        // Load the user from the database to get their info for the response.
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(); // can't be empty here — we just authenticated them

        // Generate a fresh JWT token for this session
        String token = jwtUtil.generateToken(user.getEmail());

        // Return the token and user info
        return AuthResponse.builder()
            .token(token)
            .username(user.getDisplayName())
            .email(user.getEmail())
            .baseCurrency(user.getBaseCurrency())
            .build();
    }
}
