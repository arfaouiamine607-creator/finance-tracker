package com.financetracker.repository;

import com.financetracker.entity.Category;
import com.financetracker.entity.Category.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CategoryRepository.java — Data access layer for the Category entity.
 *
 * Handles all database operations on the "categories" table.
 * Main use cases:
 *   1. Load all student categories to pre-fill the student budget dashboard
 *   2. Load categories by type (EXPENSE vs INCOME) for dropdown menus
 *   3. Look up a category by name (for data validation)
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Get all categories that are flagged as student categories.
     * These are the 8 pre-built ones: Rent, Groceries, Transport, Tuition,
     * Books, Eating Out, Entertainment, Phone/Internet.
     * Used to populate the "Student Budget" dashboard.
     *
     * Spring generates: SELECT * FROM categories WHERE is_student_category = true
     */
    List<Category> findByIsStudentCategoryTrue();

    /**
     * Get all categories of a given type (EXPENSE or INCOME).
     * Used to populate dropdown menus when adding a transaction.
     *
     * Spring generates: SELECT * FROM categories WHERE type = ?
     */
    List<Category> findByType(CategoryType type);

    /**
     * Find a category by its exact name.
     * Used to check for duplicates before creating a new category.
     *
     * Spring generates: SELECT * FROM categories WHERE name = ? LIMIT 1
     */
    Optional<Category> findByName(String name);

    /**
     * Get all student categories of a specific type.
     * e.g. all student EXPENSE categories (for the expense dashboard)
     *
     * Spring generates: SELECT * FROM categories
     *                   WHERE is_student_category = true AND type = ?
     */
    List<Category> findByIsStudentCategoryTrueAndType(CategoryType type);
}
