package com.financetracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps to the "categories" table. A category is a spending label like
 * "Groceries", "Rent", or "Transport".
 *
 * I have two kinds of categories in my app:
 *   1. Student categories (isStudentCategory = true) — 8 pre-built ones
 *      that I seed into the database on startup. These power the student
 *      budget dashboard.
 *   2. Regular categories — anything else.
 *
 * Categories are shared across all users, not per-user.
 * The type field (EXPENSE vs INCOME) controls which dropdown they appear in
 * when a user is logging a transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name is required")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Marks the 8 pre-built student categories:
    // Rent, Groceries, Transport, Tuition, Books, Eating Out, Entertainment, Phone/Internet
    @Column(name = "is_student_category", nullable = false)
    private Boolean isStudentCategory = false;

    // Hex color code used in my charts (e.g. "#FF6384")
    @Column(length = 7)
    private String color;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type = CategoryType.EXPENSE;

    public enum CategoryType {
        EXPENSE,
        INCOME
    }
}
