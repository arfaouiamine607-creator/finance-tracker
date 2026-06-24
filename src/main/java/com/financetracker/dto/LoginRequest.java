package com.financetracker.dto;

// Validation annotations — enforce that the fields are not empty
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Lombok generates getters/setters automatically
import lombok.Data;

// This class holds the data the user sends us when they log in.
// Example JSON they send:
// {
//   "email": "john@example.com",
//   "password": "secret123"
// }
// That's it — login only needs two fields.
@Data
public class LoginRequest {

    // The email address the user registered with
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    // The raw password the user typed — we'll compare it against
    // the bcrypt hash stored in the database (we never store plain text)
    @NotBlank(message = "Password is required")
    private String password;
}
