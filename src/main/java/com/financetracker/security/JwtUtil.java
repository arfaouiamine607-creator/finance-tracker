package com.financetracker.security;

// The JWT library (jjwt) — handles all the token encoding/decoding for us
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Lets Spring read our jwt.secret and jwt.expiration from application.properties
import org.springframework.beans.factory.annotation.Value;

// Marks this class as a Spring component so it can be injected anywhere
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// JwtUtil handles everything related to JWT tokens:
//   - Creating a token when a user logs in
//   - Reading the email out of a token on incoming requests
//   - Checking if a token is still valid (not expired, not tampered with)
//
// How JWT works in 3 steps:
//   1. User logs in → we create a token signed with our secret key
//   2. User sends the token with every request in the Authorization header
//   3. We verify the signature and read the email to know who the user is
@Component // tells Spring to create one instance of this class and share it everywhere
public class JwtUtil {

    // Our secret signing key — loaded from application-local.properties
    // Must be at least 32 characters. Anyone with this key can make tokens,
    // so it must NEVER be committed to GitHub.
    @Value("${app.jwt.secret}")
    private String secret;

    // How long a token lives before it expires — loaded from application.properties
    // We set it to 86400000 ms = 24 hours
    @Value("${app.jwt.expiration}")
    private long expiration;

    // Converts our plain text secret string into a cryptographic key object
    // that the JWT library can use to sign and verify tokens.
    // We call this every time instead of storing the key, which keeps things simple.
    private SecretKey getSigningKey() {
        // Turn the secret string into raw bytes using UTF-8 encoding
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Wrap those bytes in a secure HMAC-SHA key object the JWT library understands
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Creates a brand new JWT token for a user who just logged in.
    // The token contains the user's email and an expiry time, and is signed
    // with our secret key. The signature means nobody can fake or modify the token.
    public String generateToken(String email) {
        return Jwts.builder()
            .subject(email)                    // store the email inside the token
            .issuedAt(new Date())              // record when the token was created
            .expiration(new Date(System.currentTimeMillis() + expiration)) // set expiry
            .signWith(getSigningKey())         // sign it so we can detect tampering
            .compact();                        // encode everything into the final string
    }

    // Reads the email address out of a token.
    // Called on every incoming request to find out who the user is.
    public String extractEmail(String token) {
        return extractClaims(token).getSubject(); // "subject" is where we stored the email
    }

    // Checks if a token is still usable:
    //   - the email inside matches the user we're expecting
    //   - the token hasn't expired yet
    public boolean isTokenValid(String token, String email) {
        String tokenEmail = extractEmail(token); // read the email from the token
        return tokenEmail.equals(email) && !isTokenExpired(token); // both must be true
    }

    // Returns true if the token's expiry date is in the past
    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    // Decodes and verifies the token, then returns its contents (called "claims").
    // If the token was tampered with or the signature doesn't match, this throws
    // an exception automatically — we don't have to check manually.
    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey()) // use our secret key to verify the signature
            .build()
            .parseSignedClaims(token)   // decode the token string
            .getPayload();              // get the actual data stored inside
    }
}
