package com.financetracker.repository;

import com.financetracker.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BudgetRepository.java — Data access layer for the Budget entity.
 *
 * Handles all database queries for monthly budget limits.
 * Main use cases:
 *   1. Retrieve a user's budgets for a specific month (dashboard display)
 *   2. Find the budget for a specific category/month (budget progress check)
 *   3. Check if a budget already exists before creating a new one
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Get all budgets a user has set for a specific month and year.
     * This powers the monthly budget dashboard — it shows every category
     * the user budgeted for, with its limit and remaining amount.
     *
     * Spring generates: SELECT * FROM budgets
     *                   WHERE user_id = ? AND month = ? AND year = ?
     */
    List<Budget> findByUserIdAndMonthAndYear(Long userId, int month, int year);

    /**
     * Find the budget for a specific user, category, month, and year.
     * Used when checking if a user is approaching their limit for one category.
     * Returns Optional — empty if the user has no budget for that category/month.
     *
     * Spring generates: SELECT * FROM budgets
     *                   WHERE user_id = ? AND category_id = ?
     *                   AND month = ? AND year = ? LIMIT 1
     */
    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(
        Long userId, Long categoryId, int month, int year
    );

    /**
     * Check if a budget already exists for this user/category/month/year.
     * Used before creating a new budget to prevent duplicates
     * (the DB unique constraint is the safety net, but checking first gives
     * a nicer error message than a constraint violation exception).
     *
     * Spring generates: SELECT COUNT(*) > 0 FROM budgets
     *                   WHERE user_id = ? AND category_id = ?
     *                   AND month = ? AND year = ?
     */
    boolean existsByUserIdAndCategoryIdAndMonthAndYear(
        Long userId, Long categoryId, int month, int year
    );

    /**
     * Get all budgets for a user across all time.
     * Used for a full budget history view (optional feature for later).
     *
     * Spring generates: SELECT * FROM budgets WHERE user_id = ?
     *                   ORDER BY year DESC, month DESC
     */
    List<Budget> findByUserIdOrderByYearDescMonthDesc(Long userId);
}
