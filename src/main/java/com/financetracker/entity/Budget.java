package com.financetracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Budget.java — Entity class
 *
 * Represents a monthly spending limit set for one category.
 * e.g. "$400 CAD on Groceries in June 2026"
 *
 * The budget data is used to:
 *   1. Show a progress bar (spent / limit)
 *   2. Trigger a "Danger Zone" alert when spending reaches 80% of the limit
 *   3. Show an "Over Budget" warning if the limit is exceeded
 *
 * One Budget row = one user + one category + one month/year combination.
 * The unique constraint prevents setting two limits for the same
 * category in the same month.
 *
 * JPA creates a "budgets" table from this class in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "budgets",
    uniqueConstraints = {
        // A user can only have ONE budget per category per month per year.
        // This prevents duplicate budget rows at the database level.
        @UniqueConstraint(
            name = "uk_budget_user_category_month_year",
            columnNames = {"user_id", "category_id", "month", "year"}
        )
    }
)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------------------------------------------------------------------------
    // RELATIONSHIPS
    // -------------------------------------------------------------------------

    /** Which user set this budget. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Which spending category this budget applies to. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // -------------------------------------------------------------------------
    // BUDGET PERIOD
    // -------------------------------------------------------------------------

    /**
     * The month this budget applies to (1 = January, 12 = December).
     * Combined with 'year', this identifies a specific calendar month.
     */
    @NotNull
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    @Column(nullable = false)
    private Integer month;

    /**
     * The year this budget applies to (e.g. 2026).
     * Using an integer is simpler than storing a full date for a monthly budget.
     */
    @NotNull
    @Min(value = 2020, message = "Year must be 2020 or later")
    @Column(nullable = false)
    private Integer year;

    // -------------------------------------------------------------------------
    // BUDGET AMOUNT
    // -------------------------------------------------------------------------

    /**
     * The maximum amount the user wants to spend in this category this month.
     * Always stored in the user's base currency for consistent comparisons.
     * e.g. 400.00 (CAD) for Groceries in June 2026
     */
    @NotNull
    @DecimalMin(value = "1.00", message = "Budget limit must be at least 1.00")
    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    /**
     * The currency of limitAmount — always the user's base currency.
     * Stored here so it can be displayed without loading the full User object.
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /**
     * The DANGER ZONE threshold as a percentage (default: 80).
     * When (totalSpent / limitAmount) >= (alertThreshold / 100),
     * the app shows a red warning to the user.
     * Users can customize this — a careful budgeter might set it to 70%.
     */
    @Column(name = "alert_threshold", nullable = false)
    private Integer alertThreshold = 80; // warn at 80% spent by default

    /** When this budget was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** When this budget was last modified (e.g. user raised the limit). */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -------------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate // runs automatically before JPA updates an existing row
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
