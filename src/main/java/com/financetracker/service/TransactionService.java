package com.financetracker.service;

// The DTOs — what comes in from the request and what goes out in the response
import com.financetracker.dto.TransactionRequest;
import com.financetracker.dto.TransactionResponse;

// The database entities
import com.financetracker.entity.Category;
import com.financetracker.entity.Transaction;
import com.financetracker.entity.User;

// Database access layers
import com.financetracker.repository.CategoryRepository;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.repository.UserRepository;

// Marks this as a Spring service class
import org.springframework.stereotype.Service;

// Keeps the database connection open for the whole method so lazy-loaded
// relationships (like Category name) can be read without errors
import org.springframework.transaction.annotation.Transactional;

// For throwing HTTP errors with the right status code
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

// TransactionService handles all the logic around creating, listing, and deleting transactions.
//
// Each method receives the logged-in user's email (extracted from the JWT in the controller).
// We use the email to load the User from the database, so we always know who is making the request.
//
// Security: when fetching or deleting by ID, we always verify the transaction belongs
// to the logged-in user — so user A can never touch user B's data.
@Service
@Transactional // keeps the DB session open so lazy-loaded Category fields work inside toResponse()
public class TransactionService {

    // Loads transactions from the database
    private final TransactionRepository transactionRepository;

    // Loads users from the database (to find who's logged in)
    private final UserRepository userRepository;

    // Loads categories from the database (to validate the categoryId in the request)
    private final CategoryRepository categoryRepository;

    // Converts money between currencies (e.g. 100 MAD → 13.72 CAD)
    private final ExchangeRateService exchangeRateService;

    // Spring injects all 4 dependencies through the constructor
    public TransactionService(
        TransactionRepository transactionRepository,
        UserRepository userRepository,
        CategoryRepository categoryRepository,
        ExchangeRateService exchangeRateService
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.exchangeRateService = exchangeRateService;
    }

    // Saves a new transaction to the database.
    //
    // Steps:
    //   1. Load the user making the request (by their email from the JWT)
    //   2. Validate the category ID they sent exists
    //   3. Convert the amount to the user's base currency
    //   4. Build and save the Transaction entity
    //   5. Return a TransactionResponse with all the saved data
    public TransactionResponse addTransaction(TransactionRequest request, String userEmail) {

        // Load the user from the database using their email
        // (the email comes from the JWT token, which the controller extracts)
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Load the category they chose (e.g. categoryId=2 = "Groceries")
        // If the ID doesn't exist, return 404 so the client knows it was a bad request
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Category not found with id: " + request.getCategoryId()));

        // Convert the amount to the user's base currency.
        // Example: user typed 100 MAD, their base currency is CAD
        //   → exchangeRateService.convert(100, "MAD", "CAD") → 13.72
        // If the currency is the same as base (e.g. they typed CAD and base is CAD),
        // the service returns the amount unchanged — no API call needed.
        var convertedAmount = exchangeRateService.convert(
            request.getAmount(),         // original amount the user typed
            request.getCurrency(),       // currency they typed it in
            user.getBaseCurrency()       // their base currency (stored on their account)
        );

        // Get the exchange rate that was used, so we can store it for audit purposes.
        // e.g. "1 MAD = 0.1372 CAD" → we store 0.1372
        // If same currency, rate is 1.0 (no conversion happened)
        var exchangeRate = request.getCurrency().equalsIgnoreCase(user.getBaseCurrency())
            ? java.math.BigDecimal.ONE
            : exchangeRateService.getRate(request.getCurrency(), user.getBaseCurrency());

        // Build the Transaction entity from the request data + calculated values
        Transaction transaction = Transaction.builder()
            .user(user)                                        // who made this transaction
            .category(category)                                // which category it belongs to
            .amount(request.getAmount())                       // original amount the user typed
            .currency(request.getCurrency().toUpperCase())     // uppercase the currency code
            .convertedAmount(convertedAmount)                  // calculated by exchange rate service
            .baseCurrency(user.getBaseCurrency())              // snapshot of user's base currency now
            .exchangeRate(exchangeRate)                        // rate used for conversion
            .type(Transaction.TransactionType.valueOf(request.getType().toUpperCase())) // "EXPENSE" or "INCOME"
            .date(request.getDate())                           // the date the money moved
            .description(request.getDescription())             // optional note from the user
            .build();
        // Note: createdAt is set automatically by @PrePersist in the Transaction entity

        // Save to the database — Hibernate generates the INSERT statement
        Transaction saved = transactionRepository.save(transaction);

        // Convert the saved entity to a response DTO and return it
        return toResponse(saved);
    }

    // Returns all transactions for the logged-in user, newest first.
    // Used by GET /api/transactions
    public List<TransactionResponse> getAllTransactions(String userEmail) {

        // Load the user to get their ID (we query by user ID, not email)
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Fetch all transactions for this user, sorted newest first
        return transactionRepository.findByUserIdOrderByDateDesc(user.getId())
            .stream()
            .map(this::toResponse) // convert each Transaction entity to a TransactionResponse
            .toList();             // collect into a List
    }

    // Returns one specific transaction by its database ID.
    // Used by GET /api/transactions/{id}
    //
    // Security: we verify the transaction belongs to the logged-in user.
    // If user A tries to fetch user B's transaction, they get 403 Forbidden.
    public TransactionResponse getTransaction(Long id, String userEmail) {

        // Load the user to know who is asking
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Find the transaction by ID — 404 if it doesn't exist
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Transaction not found with id: " + id));

        // Security check: make sure this transaction belongs to the logged-in user
        // transaction.getUser().getId() → the owner's ID
        // user.getId() → the logged-in user's ID
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You do not have access to this transaction");
        }

        return toResponse(transaction);
    }

    // Deletes a transaction permanently.
    // Used by DELETE /api/transactions/{id}
    //
    // Security: same ownership check as getTransaction — users can only delete their own.
    public void deleteTransaction(Long id, String userEmail) {

        // Load the user to know who is asking
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Find the transaction — 404 if it doesn't exist
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Transaction not found with id: " + id));

        // Security check: make sure the logged-in user owns this transaction
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You do not have access to this transaction");
        }

        // Delete the transaction from the database
        transactionRepository.delete(transaction);
    }

    // Private helper: converts a Transaction entity (database row) to a TransactionResponse (JSON).
    // Called after saving and when listing/fetching transactions.
    //
    // We keep this as a private method because it's only used inside this service.
    // It avoids repeating the same mapping code in every public method above.
    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
            .id(t.getId())
            .amount(t.getAmount())
            .currency(t.getCurrency())
            .convertedAmount(t.getConvertedAmount())
            .baseCurrency(t.getBaseCurrency())
            .exchangeRate(t.getExchangeRate())
            .type(t.getType().name())              // enum → string (e.g. EXPENSE → "EXPENSE")
            .categoryId(t.getCategory().getId())   // lazy-loaded — works because of @Transactional
            .categoryName(t.getCategory().getName()) // same — lazy-loaded safely inside a transaction
            .date(t.getDate())
            .description(t.getDescription())
            .createdAt(t.getCreatedAt())
            .build();
    }
}
