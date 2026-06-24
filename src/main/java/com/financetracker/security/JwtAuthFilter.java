package com.financetracker.security;

// Needed to load the user from the database by their email
import com.financetracker.repository.UserRepository;

// The base class for all JWT errors (expired, malformed, wrong signature, etc.)
import io.jsonwebtoken.JwtException;

// Standard Java IO imports for reading the HTTP request and response
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Spring Security classes for telling Spring "this user is authenticated"
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

// Marks this as a Spring component so it gets picked up automatically
import org.springframework.stereotype.Component;

// Makes this a filter that Spring applies to every HTTP request
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// This filter runs once on every single incoming HTTP request — before any controller.
// Its job: check if the request has a valid JWT token in the Authorization header.
//   - If yes  → figure out who the user is and tell Spring Security
//   - If no   → do nothing, let Spring Security block it later if the route needs auth
//
// The Authorization header looks like this:
//   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
//                  ^^^^^^ ^^^^^^^^^^^^^^^^^^^^^^^^
//                  prefix    the actual JWT token
@Component // Spring creates one instance of this and runs it on every request
public class JwtAuthFilter extends OncePerRequestFilter {

    // Used to verify the token's signature and read the email from it
    private final JwtUtil jwtUtil;

    // Used to load the full User object from the database by their email
    private final UserRepository userRepository;

    // Spring injects both dependencies through the constructor automatically
    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // This method runs for every HTTP request.
    // filterChain.doFilter() at the end passes the request to the next step
    // (either another filter, or the actual controller method).
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,    // the incoming HTTP request
        HttpServletResponse response,  // the outgoing HTTP response
        FilterChain filterChain        // the rest of the chain to continue to
    ) throws ServletException, IOException {

        // Read the Authorization header from the request
        // e.g. "Bearer eyJhbGciOiJIUzI1NiJ9..."
        String authHeader = request.getHeader("Authorization");

        // If the header is missing or doesn't start with "Bearer ",
        // this request has no token — skip our logic and move on.
        // Spring Security will decide later whether to block this request.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // pass to the next filter/controller
            return;                                  // stop processing here
        }

        // Chop off the "Bearer " prefix (7 characters) to get just the token string
        String token = authHeader.substring(7);

        // Read the email address out of the token.
        // We wrap this in try-catch because jwtUtil.extractEmail() throws JwtException
        // if the token is malformed, expired, or has a bad signature.
        // Without this catch, a garbage token causes a 500 server error.
        // With it, we just skip authentication and let Spring Security return 403.
        String email;
        try {
            email = jwtUtil.extractEmail(token);
        } catch (JwtException e) {
            // Token is invalid — skip authentication entirely.
            // Spring Security will block the request with 403 if the route needs auth.
            filterChain.doFilter(request, response);
            return;
        }

        // Only proceed if:
        //   1. We got a real email from the token (token wasn't empty/garbage)
        //   2. Spring Security doesn't already have a user authenticated for this request
        //      (avoids re-authenticating on the same request if something already ran)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load the full User object from the database using the email in the token
            // findByEmail returns an Optional — if nobody has that email, skip authentication
            UserDetails user = userRepository.findByEmail(email).orElse(null);

            // Check that:
            //   - we actually found a user with that email in the database
            //   - the token is still valid (not expired, signature matches)
            if (user != null && jwtUtil.isTokenValid(token, email)) {

                // Create an authentication object that Spring Security understands.
                // This is basically saying "this user is verified and logged in".
                // null for credentials — we don't need the password anymore at this point.
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        user,                  // the authenticated user object
                        null,                  // credentials (not needed after token check)
                        user.getAuthorities()  // their roles (e.g. ROLE_USER, ROLE_ADMIN)
                    );

                // Attach extra request details (like the IP address) to the auth token
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Store this authentication in the current request's security context.
                // From this point on, Spring Security knows who this user is for this request.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Pass the request along to the next filter or the controller
        filterChain.doFilter(request, response);
    }
}
