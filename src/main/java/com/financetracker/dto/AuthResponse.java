package com.financetracker.dto;

// Lombok generates the constructor, getters, and builder for us
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

// This is what we send BACK to the user after they register or log in.
// Example JSON we return:
// {
//   "token": "eyJhbGciOiJIUzI1NiJ9...",
//   "username": "john_doe",
//   "email": "john@example.com",
//   "baseCurrency": "CAD"
// }
//
// The token is the important part — the user must send it with every
// future request so we know who they are without asking them to log in again.
// It goes in the HTTP header like this:
//   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
@Data
@Builder            // lets us write AuthResponse.builder().token("...").build()
@AllArgsConstructor // generates a constructor that takes all 4 fields at once
public class AuthResponse {

    // The JWT token — a long encoded string the user keeps and sends with every request.
    // It expires after 24 hours (set in application.properties).
    private String token;

    // The user's display name — shown in the app UI (e.g. "Welcome back, john_doe!")
    private String username;

    // The user's email — useful for the frontend to show in the profile section
    private String email;

    // The user's chosen currency — the frontend needs this to know how to display amounts
    private String baseCurrency;
}
