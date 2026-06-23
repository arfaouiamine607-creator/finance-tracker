package com.financetracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transaction.java — Entity class
 *
 * Represents a single financial event — either an expense or income.
 * This is the core table of my app. Every time a user logs money spent
 * or money received, a Transaction row is created.
 *
 * Multi-currency design:
 *   - 'amount'          = the original amount entered (e.g. 50.00 in MAD)
 *   - 'currency'        = the currency it was entered in (e.g. "MAD")
 *   - 'convertedAmount' = the equivalent in the user's base currency (e.g. 6.87 CAD)
 *   - 'baseCurrency'    = the user's base currency at the time of the transaction
 *
 * Storing both preserves the original entry AND enables consistent reporting.
 *
 * JPA creates a "transactions" table from this class in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------------------------------------------------------------------------
    // RELATIONSHIPS — links to other tables via foreign keys
    // -------------------------------------------------------------------------

    /**
     * Which user made this transaction.
     * @ManyToOne — many transactions can belong to one user.
     * LAZY = only load User data from DB when explicitly accessed (performance).
     * @JoinColumn — the foreign key column in the "transactions" table is "user_id".
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * What category this transaction belongs to (e.g. "Groceries").
     * @ManyToOne — many transactions can share one category.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // -------------------------------------------------------------------------
    // FINANCIAL DATA
    // -------------------------------------------------------------------------

    /**
     * The amount entered, in the currency the user chose.
     * I use BigDecimal (not double/float) for money — floating point math
     * causes rounding errors that are unacceptable in financial software.
     * e.g. BigDecimal("49.99") not 49.989999999998
     *
     * precision = total digits, scale = digits after decimal point
     * precision=19, scale=4 supports amounts up to 999,999,999,999,999.9999
     */
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * The currency code for 'amount' (ISO 4217 format).
     * e.g. "CAD", "USD", "EUR", "MAD", "GBP"
     * Always 3 uppercase letters.
     */
    @NotNull
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * The amount converted to the user's base currency.
     * Calculated automatically using the exchange rate API when saving.
     * Used for budget tracking and charts (consistent currency for comparison).
     */
    @Column(name = "converted_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal convertedAmount;

    /**
     * The user's base currency at the time of this transaction.
     * Stored here because the user could change their base currency later —
     * I want to preserve what it was when this transaction was originally logged.
     */
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    /**
     * The exchange rate used for conversion (amount * exchangeRate = convertedAmount).
     * Stored for audit purposes so the conversion can always be verified later.
     * e.g. if amount=100 MAD and convertedAmount=13.72 CAD, rate=0.1372
     */
    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    /**
     * EXPENSE = money going out (a bill, a purchase)
     * INCOME  = money coming in (salary, scholarship, freelance)
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /**
     * The date the transaction actually happened (not when it was logged).
     * LocalDate = date only, no time component (just year-month-day).
     * The user might log last week's grocery run — that's normal.
     */
    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    /**
     * Optional note the user writes to remember what this was.
     * e.g. "Weekly groceries at IGA" or "Metro pass October"
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(length = 500)
    private String description;

    /**
     * When this record was inserted into the database.
     * Different from 'date' — 'createdAt' is the logging timestamp,
     * 'date' is when the money actually moved.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // -------------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // TRANSACTION TYPE ENUM
    // -------------------------------------------------------------------------

    public enum TransactionType {
        EXPENSE, // money leaving the user's wallet
        INCOME   // money entering the user's wallet
    }
}
