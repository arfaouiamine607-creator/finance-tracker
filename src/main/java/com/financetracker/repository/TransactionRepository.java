package com.financetracker.repository;

import com.financetracker.entity.Transaction;
import com.financetracker.entity.Transaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * TransactionRepository.java — Data access layer for the Transaction entity.
 *
 * All database queries related to transactions are defined here.
 * This is the most query-heavy repository because transactions power:
 *   - The main expense list (filtered, sorted, paginated)
 *   - Budget progress calculation (total spent per category per month)
 *   - Charts and Power BI reports (spending by category, by month, trends)
 *   - Income vs expense summaries
 *
 * Mix of two query styles:
 *   1. Method name queries (Spring derives SQL from the method name)
 *   2. @Query with JPQL (Java Persistence Query Language — like SQL but for entities)
 *      Used when the query is too complex to express as a method name.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // -------------------------------------------------------------------------
    // BASIC LOOKUPS
    // -------------------------------------------------------------------------

    /**
     * Get all transactions for a specific user, newest first.
     * The main query behind the "My Transactions" page.
     *
     * Spring generates: SELECT * FROM transactions WHERE user_id = ?
     *                   ORDER BY date DESC
     */
    List<Transaction> findByUserIdOrderByDateDesc(Long userId);

    /**
     * Get all transactions for a user in a date range.
     * Used for monthly reports and custom date filtering.
     * 'Between' in Spring Data is inclusive on both ends.
     *
     * Spring generates: SELECT * FROM transactions
     *                   WHERE user_id = ? AND date BETWEEN ? AND ?
     *                   ORDER BY date DESC
     */
    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(
        Long userId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Get all transactions for a user in a specific category.
     * Used when the user filters by category in the UI.
     *
     * Spring generates: SELECT * FROM transactions
     *                   WHERE user_id = ? AND category_id = ?
     *                   ORDER BY date DESC
     */
    List<Transaction> findByUserIdAndCategoryIdOrderByDateDesc(Long userId, Long categoryId);

    /**
     * Get only EXPENSE or only INCOME transactions for a user.
     * Used for the "Expenses" vs "Income" tab split in the UI.
     */
    List<Transaction> findByUserIdAndTypeOrderByDateDesc(Long userId, TransactionType type);

    // -------------------------------------------------------------------------
    // AGGREGATIONS — for budget tracking and dashboard numbers
    // -------------------------------------------------------------------------

    /**
     * Calculate total EXPENSE spending for a user in a specific category
     * during a given month/year. This is the core query for budget progress bars.
     *
     * I use @Query with JPQL here because the logic (SUM + filter by month/year)
     * is too complex to express as a Spring Data method name.
     *
     * JPQL uses entity class names (Transaction) and field names (t.date),
     * not table/column names. FUNCTION('EXTRACT',...) calls a native DB function.
     *
     * Returns null if there are no matching transactions (COALESCE handles this).
     */
    @Query("""
        SELECT COALESCE(SUM(t.convertedAmount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.category.id = :categoryId
          AND t.type = 'EXPENSE'
          AND FUNCTION('EXTRACT', 'MONTH', t.date) = :month
          AND FUNCTION('EXTRACT', 'YEAR', t.date) = :year
        """)
    BigDecimal sumExpensesByUserAndCategoryAndMonth(
        @Param("userId") Long userId,
        @Param("categoryId") Long categoryId,
        @Param("month") int month,
        @Param("year") int year
    );

    /**
     * Get total spending per category for a user in a given month.
     * Returns a list of Object[] arrays: [categoryId, categoryName, total]
     * Used for pie charts and the category breakdown dashboard.
     *
     * This is a "projection query" — it doesn't return full Transaction objects,
     * just the aggregated data we need for charts.
     */
    @Query("""
        SELECT t.category.id, t.category.name, SUM(t.convertedAmount)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = 'EXPENSE'
          AND FUNCTION('EXTRACT', 'MONTH', t.date) = :month
          AND FUNCTION('EXTRACT', 'YEAR', t.date) = :year
        GROUP BY t.category.id, t.category.name
        ORDER BY SUM(t.convertedAmount) DESC
        """)
    List<Object[]> sumExpensesByCategoryForMonth(
        @Param("userId") Long userId,
        @Param("month") int month,
        @Param("year") int year
    );

    /**
     * Get monthly spending totals for the last N months.
     * Used for the "spending trend" line chart.
     * Returns [year, month, total] for each month that has transactions.
     */
    @Query("""
        SELECT FUNCTION('EXTRACT', 'YEAR', t.date),
               FUNCTION('EXTRACT', 'MONTH', t.date),
               SUM(t.convertedAmount)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = 'EXPENSE'
          AND t.date >= :startDate
        GROUP BY FUNCTION('EXTRACT', 'YEAR', t.date),
                 FUNCTION('EXTRACT', 'MONTH', t.date)
        ORDER BY FUNCTION('EXTRACT', 'YEAR', t.date),
                 FUNCTION('EXTRACT', 'MONTH', t.date)
        """)
    List<Object[]> sumMonthlyExpensesSince(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate
    );

    /**
     * Total income for a user in a given month.
     * Used to display "Income this month: $X" on the dashboard.
     */
    @Query("""
        SELECT COALESCE(SUM(t.convertedAmount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = 'INCOME'
          AND FUNCTION('EXTRACT', 'MONTH', t.date) = :month
          AND FUNCTION('EXTRACT', 'YEAR', t.date) = :year
        """)
    BigDecimal sumIncomeByUserAndMonth(
        @Param("userId") Long userId,
        @Param("month") int month,
        @Param("year") int year
    );
}
