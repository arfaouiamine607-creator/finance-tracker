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
 *   1. Method name queries — Spring derives the SQL from the method name automatically.
 *      e.g. findByUserIdOrderByDateDesc → SELECT * FROM transactions WHERE user_id = ? ORDER BY date DESC
 *
 *   2. @Query with nativeQuery = true — raw PostgreSQL SQL written by hand.
 *      Used for complex aggregations (SUM, GROUP BY, EXTRACT) that can't be
 *      expressed as a method name. nativeQuery = true means we write real SQL
 *      (column names, table names) instead of JPQL (entity/field names).
 *      This avoids Hibernate's enum-parsing limitations in HQL.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // -------------------------------------------------------------------------
    // BASIC LOOKUPS — Spring generates these queries from the method name
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
    // AGGREGATIONS — native PostgreSQL SQL for complex SUM/GROUP BY queries
    //
    // Why nativeQuery = true?
    //   Hibernate's HQL (the JPQL-like language) can't reference enum values
    //   defined as inner classes (e.g. Transaction.TransactionType.EXPENSE).
    //   Native SQL sidesteps this completely: since @Enumerated(EnumType.STRING)
    //   stores the enum as the text "EXPENSE" or "INCOME" in the database,
    //   we can simply write type = 'EXPENSE' in our SQL and it works perfectly.
    //
    // COALESCE(SUM(...), 0):
    //   SUM returns NULL when there are no matching rows. COALESCE replaces NULL
    //   with 0, so the caller never has to handle a null BigDecimal.
    // -------------------------------------------------------------------------

    /**
     * Total EXPENSE spending for a user in one category during a specific month.
     * This is the core query for budget progress bars:
     *   spent = sumExpensesByUserAndCategoryAndMonth(...)
     *   progress = spent / budget.limitAmount * 100
     *
     * EXTRACT(MONTH FROM date) — pulls just the month number out of the date column.
     * EXTRACT(YEAR FROM date)  — pulls just the year.
     * e.g. for date 2026-06-15: MONTH=6, YEAR=2026
     */
    @Query(value = """
        SELECT COALESCE(SUM(converted_amount), 0)
        FROM transactions
        WHERE user_id     = :userId
          AND category_id = :categoryId
          AND type        = 'EXPENSE'
          AND EXTRACT(MONTH FROM date) = :month
          AND EXTRACT(YEAR  FROM date) = :year
        """, nativeQuery = true)
    BigDecimal sumExpensesByUserAndCategoryAndMonth(
        @Param("userId")     Long userId,
        @Param("categoryId") Long categoryId,
        @Param("month")      int month,
        @Param("year")       int year
    );

    /**
     * Total spending per category for a user in a given month.
     * Returns a list where each row is: [category_id, category_name, total_spent]
     * Used for the pie chart on the dashboard (which category ate the most budget?).
     *
     * This is called a "projection query" — it returns summary data, not full
     * Transaction rows. Each Object[] element maps to one row from the SQL result.
     *
     * JOIN categories c ON t.category_id = c.id:
     *   We JOIN the categories table so we can include c.name in the result.
     *   Without the JOIN, we'd only have the ID and couldn't show the category name.
     */
    @Query(value = """
        SELECT t.category_id, c.name, SUM(t.converted_amount)
        FROM transactions t
        JOIN categories c ON t.category_id = c.id
        WHERE t.user_id = :userId
          AND t.type    = 'EXPENSE'
          AND EXTRACT(MONTH FROM t.date) = :month
          AND EXTRACT(YEAR  FROM t.date) = :year
        GROUP BY t.category_id, c.name
        ORDER BY SUM(t.converted_amount) DESC
        """, nativeQuery = true)
    List<Object[]> sumExpensesByCategoryForMonth(
        @Param("userId") Long userId,
        @Param("month")  int month,
        @Param("year")   int year
    );

    /**
     * Monthly expense totals since a given start date.
     * Returns one row per month: [year, month, total_spent]
     * Used to draw the spending trend line chart (last 6 months, etc.).
     *
     * GROUP BY EXTRACT(YEAR FROM date), EXTRACT(MONTH FROM date):
     *   This collapses all transactions in the same month into one total.
     *   e.g. June 2026 → one row with year=2026, month=6, total=XYZ
     */
    @Query(value = """
        SELECT EXTRACT(YEAR  FROM date) AS year,
               EXTRACT(MONTH FROM date) AS month,
               SUM(converted_amount)    AS total
        FROM transactions
        WHERE user_id = :userId
          AND type    = 'EXPENSE'
          AND date   >= :startDate
        GROUP BY EXTRACT(YEAR  FROM date),
                 EXTRACT(MONTH FROM date)
        ORDER BY EXTRACT(YEAR  FROM date),
                 EXTRACT(MONTH FROM date)
        """, nativeQuery = true)
    List<Object[]> sumMonthlyExpensesSince(
        @Param("userId")    Long userId,
        @Param("startDate") LocalDate startDate
    );

    /**
     * Total INCOME received by a user in a given month.
     * Used to show "Income this month: $X,XXX" on the dashboard summary card.
     * Paired with total expenses to compute net savings for the month.
     */
    @Query(value = """
        SELECT COALESCE(SUM(converted_amount), 0)
        FROM transactions
        WHERE user_id = :userId
          AND type    = 'INCOME'
          AND EXTRACT(MONTH FROM date) = :month
          AND EXTRACT(YEAR  FROM date) = :year
        """, nativeQuery = true)
    BigDecimal sumIncomeByUserAndMonth(
        @Param("userId") Long userId,
        @Param("month")  int month,
        @Param("year")   int year
    );
}
