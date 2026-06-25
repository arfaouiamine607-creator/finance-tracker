package com.financetracker.controller;

import com.financetracker.dto.TransactionRequest;
import com.financetracker.dto.TransactionResponse;
import com.financetracker.service.TransactionService;

// @Valid triggers the validation annotations on TransactionRequest (e.g. @NotNull, @DecimalMin)
import jakarta.validation.Valid;

// Spring MVC annotations for mapping HTTP requests to methods
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Authentication gives us access to the logged-in user's details
import org.springframework.security.core.Authentication;

import java.util.List;

// TransactionController exposes the transactions API.
//
// All endpoints require a valid JWT token in the Authorization header.
// Spring Security enforces this — requests without a token get 401/403 automatically.
//
// We get the logged-in user's email from the Authentication object,
// which Spring Security populates automatically from the JWT filter.
//
// Endpoints:
//   POST   /api/transactions        — log a new expense or income
//   GET    /api/transactions        — list all your transactions, newest first
//   GET    /api/transactions/{id}   — get one transaction by ID
//   DELETE /api/transactions/{id}   — delete one transaction
@RestController // handles HTTP requests and automatically serializes return values to JSON
@RequestMapping("/api/transactions") // all methods in this class share this base URL
public class TransactionController {

    // The service that contains all the business logic
    private final TransactionService transactionService;

    // Spring injects the service through the constructor
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // POST /api/transactions
    // Logs a new transaction (expense or income).
    //
    // Request body (JSON):
    //   { "amount": 50.00, "currency": "MAD", "type": "EXPENSE",
    //     "categoryId": 2, "date": "2026-06-20", "description": "Groceries" }
    //
    // Response: the saved transaction with converted amount, 201 Created
    //
    // Authentication is how Spring Security tells us who sent this request.
    // authentication.getName() returns the email stored in the JWT token.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // return 201 instead of the default 200
    public TransactionResponse addTransaction(
        @Valid @RequestBody TransactionRequest request, // @Valid checks the annotations on TransactionRequest
        Authentication authentication                   // injected by Spring — contains the logged-in user's email
    ) {
        // authentication.getName() returns the email that was encoded in the JWT
        // (JwtAuthFilter set this up — see JwtAuthFilter.java)
        String userEmail = authentication.getName();
        return transactionService.addTransaction(request, userEmail);
    }

    // GET /api/transactions
    // Returns all transactions for the logged-in user, newest first.
    //
    // Response: a JSON array of transaction objects, 200 OK
    @GetMapping
    public List<TransactionResponse> getAllTransactions(Authentication authentication) {
        String userEmail = authentication.getName();
        return transactionService.getAllTransactions(userEmail);
    }

    // GET /api/transactions/{id}
    // Returns one specific transaction by its ID.
    //
    // If the transaction doesn't exist → 404
    // If it belongs to a different user → 403
    @GetMapping("/{id}")
    public TransactionResponse getTransaction(
        @PathVariable Long id,         // {id} from the URL is injected here
        Authentication authentication
    ) {
        String userEmail = authentication.getName();
        return transactionService.getTransaction(id, userEmail);
    }

    // DELETE /api/transactions/{id}
    // Permanently deletes a transaction.
    //
    // If the transaction doesn't exist → 404
    // If it belongs to a different user → 403
    // On success → 204 No Content (standard for DELETE — nothing to return)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
        @PathVariable Long id,
        Authentication authentication
    ) {
        String userEmail = authentication.getName();
        transactionService.deleteTransaction(id, userEmail);
        // 204 No Content — the delete worked, there's nothing to return in the body
        return ResponseEntity.noContent().build();
    }
}
