package com.financetracker.dto;

// Lombok: @Data = getters/setters, @Builder = builder pattern, @AllArgsConstructor = constructor with all fields
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// TransactionResponse is what we send back to the user (or frontend) after
// saving a transaction, or when they ask to list their transactions.
//
// It's a flat, simple object — no nested User or Category objects.
// We only include what the frontend actually needs to display.
//
// Example response:
//   {
//     "id": 1,
//     "amount": 100.00,
//     "currency": "MAD",
//     "convertedAmount": 13.72,
//     "baseCurrency": "CAD",
//     "exchangeRate": 0.137200,
//     "type": "EXPENSE",
//     "categoryId": 2,
//     "categoryName": "Groceries",
//     "date": "2026-06-20",
//     "description": "Weekly groceries",
//     "createdAt": "2026-06-20T14:35:00"
//   }
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    // The database ID of this transaction (useful for DELETE /api/transactions/{id})
    private Long id;

    // The original amount the user entered, in the currency they chose
    private BigDecimal amount;

    // The currency the user entered the amount in (e.g. "MAD")
    private String currency;

    // The amount converted to the user's base currency (e.g. 13.72 CAD)
    // This is what gets used for budgets and charts — consistent currency
    private BigDecimal convertedAmount;

    // The user's base currency at the time of this transaction (e.g. "CAD")
    private String baseCurrency;

    // The exchange rate that was used: amount * exchangeRate = convertedAmount
    // Stored so the user can verify the conversion if they want
    private BigDecimal exchangeRate;

    // "EXPENSE" or "INCOME" — returned as a plain string for easy frontend display
    private String type;

    // The ID of the category (useful if the frontend needs to filter by category)
    private Long categoryId;

    // The human-readable category name (e.g. "Groceries") — frontend can show this directly
    // without a second API call to look up the category
    private String categoryName;

    // The date the transaction happened (not when it was logged)
    private LocalDate date;

    // The optional note the user wrote when logging this transaction
    private String description;

    // When this record was saved to the database
    // Shown in transaction history as "logged on ..."
    private LocalDateTime createdAt;
}
