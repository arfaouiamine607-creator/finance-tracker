package com.financetracker.dto;

// Lombok generates getters, setters, toString, equals automatically
import lombok.Data;

// Validation annotations — Spring checks these before calling the controller method
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// TransactionRequest is the JSON body the user sends when adding a new transaction.
// Example request:
//   {
//     "amount": 50.00,
//     "currency": "MAD",
//     "type": "EXPENSE",
//     "categoryId": 2,
//     "date": "2026-06-20",
//     "description": "Weekly groceries"
//   }
//
// The user does NOT send convertedAmount — we calculate that ourselves
// using the exchange rate API. That way the user just types what they spent
// and we handle the conversion transparently.
@Data // generates getters + setters for all fields below
public class TransactionRequest {

    // The money amount the user actually spent or received
    // Must be at least 0.01 — we reject zero or negative amounts
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    // The 3-letter currency code for the amount above
    // e.g. "CAD", "USD", "EUR", "MAD" — must be exactly 3 letters (ISO 4217)
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code like CAD or USD")
    private String currency;

    // Whether this is money going out (EXPENSE) or money coming in (INCOME)
    // Must be exactly "EXPENSE" or "INCOME" — Spring maps the string to the enum
    @NotNull(message = "Type is required")
    private String type;

    // The ID of the category this transaction belongs to (e.g. 2 = Groceries)
    // The category must already exist in the categories table (seeded in data.sql)
    @NotNull(message = "Category is required")
    private Long categoryId;

    // The date the money actually moved — not today's date necessarily
    // e.g. the user might log a purchase from last week: "2026-06-17"
    // Spring automatically converts the ISO string "2026-06-20" to LocalDate
    @NotNull(message = "Date is required")
    private LocalDate date;

    // An optional note the user writes to remember what this was
    // e.g. "Weekly groceries at IGA" or "Metro pass for October"
    // Not required — can be left empty
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
