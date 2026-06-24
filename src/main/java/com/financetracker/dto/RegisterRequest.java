package com.financetracker.dto;

// These imports give us validation annotations like @NotBlank and @Email
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Lombok generates all the getters/setters so we don't have to type them
import lombok.Data;

// This class holds the data the user sends us when they sign up.
// Example JSON they send:
// {
//   "username": "john_doe",
//   "email": "john@example.com",
//   "password": "secret123",
//   "baseCurrency": "CAD"
// }
// Spring automatically converts that JSON into this Java object before
// our register method even runs.
@Data // generates getters, setters, toString, equals, hashCode
public class RegisterRequest {

    // The display name the user picks (shown in the app, not used for login)
    @NotBlank(message = "Username is required")           // rejects null, "", or "   "
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    private String username;

    // The email address — this is what the user types to log in
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address") // checks for @ and a domain
    private String email;

    // The password the user chooses — we never store this raw,
    // we'll run it through bcrypt before saving it to the database
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    // The currency the user wants all their money displayed in.
    // We default to "CAD" since the app targets UQAM students in Montreal.
    // The user can also send "USD", "EUR", "MAD", etc.
    private String baseCurrency = "CAD";
}
