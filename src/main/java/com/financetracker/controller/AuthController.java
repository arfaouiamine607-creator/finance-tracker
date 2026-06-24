package com.financetracker.controller;

// The DTOs — what comes in (request) and what goes out (response)
import com.financetracker.dto.AuthResponse;
import com.financetracker.dto.LoginRequest;
import com.financetracker.dto.RegisterRequest;

// The service that contains the actual register/login logic
import com.financetracker.service.AuthService;

// Spring Web annotations for building REST endpoints
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Triggers our validation annotations (@NotBlank, @Email, @Size) on the request body
import jakarta.validation.Valid;

// AuthController exposes two endpoints:
//   POST /api/auth/register  → create a new account
//   POST /api/auth/login     → log in and get a token
//
// Both routes are marked as public in SecurityConfig so no token is needed to call them.
// That's the whole point — you need these to GET a token in the first place.
@RestController  // marks this as a REST controller — return values become JSON automatically
@RequestMapping("/auth") // all routes in this class start with /auth
                         // combined with the /api prefix from application.properties:
                         // → full URL is http://localhost:8080/api/auth/...
public class AuthController {

    // The service that does the actual work (database checks, hashing, token creation)
    private final AuthService authService;

    // Spring injects AuthService through the constructor
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/register
    // Creates a new user account.
    //
    // The client sends JSON like:
    //   { "username": "john", "email": "john@x.com", "password": "abc123", "baseCurrency": "CAD" }
    //
    // We respond with:
    //   { "token": "eyJ...", "username": "john", "email": "john@x.com", "baseCurrency": "CAD" }
    //
    // HTTP 200 OK on success.
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
        @Valid @RequestBody RegisterRequest request  // @Valid triggers our field-level validation
                                                     // @RequestBody tells Spring to read the JSON body
    ) {
        // Hand off to the service — it saves the user and returns the token
        AuthResponse response = authService.register(request);

        // Wrap the response in HTTP 200 OK
        return ResponseEntity.ok(response);
    }

    // POST /api/auth/login
    // Logs in with an existing account and returns a fresh JWT token.
    //
    // The client sends JSON like:
    //   { "email": "john@x.com", "password": "abc123" }
    //
    // We respond with:
    //   { "token": "eyJ...", "username": "john", "email": "john@x.com", "baseCurrency": "CAD" }
    //
    // HTTP 200 OK on success, HTTP 403 if credentials are wrong (Spring handles that automatically).
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        // Hand off to the service — it verifies the password and returns a new token
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}
