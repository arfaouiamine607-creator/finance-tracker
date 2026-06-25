-- data.sql — runs automatically every time the app starts
-- Spring Boot reads this file because of: spring.sql.init.mode=always
-- in application.properties.
--
-- ON CONFLICT (name) DO NOTHING means:
--   "if a category with this name already exists, skip it silently"
-- This makes the file safe to run repeatedly — it never inserts duplicates.
-- The categories table has a UNIQUE constraint on the name column,
-- so PostgreSQL uses that to detect the conflict.

-- =====================================================================
-- STUDENT EXPENSE CATEGORIES (isStudentCategory = true)
-- These 8 categories power the Student Budget dashboard.
-- Colors are used in charts (hex codes).
-- =====================================================================

INSERT INTO categories (name, type, is_student_category, color)
VALUES ('Rent', 'EXPENSE', true, '#FF6384')
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, type, is_student_category, color)
VALUES ('Groceries', 'EXPENSE', true, '#36A2EB')
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, type, is_student_category, color)
VALUES ('Transport', 'EXPENSE', true, '#FFCE56')
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, type, is_student_category, color)
VALUES ('Tuition', 'EXPENSE', true, '#4BC0C0')
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, type, is_student_category, color)
VALUES ('Books', 'EXPENSE', true, '#9966FF')
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, type, is_student_category, color)
VALUES ('Eating Out', 'EXPENSE', true, '#FF9F40')
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, type, is_student_category, color)
VALUES ('Entertainment', 'EXPENSE', true, '#FF6384')
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, type, is_student_category, color)
VALUES ('Phone/Internet', 'EXPENSE', true, '#C9CBCF')
ON CONFLICT (name) DO NOTHING;

-- =====================================================================
-- INCOME CATEGORY (isStudentCategory = false)
-- Used when the user logs salary, scholarship, freelance work, etc.
-- =====================================================================

INSERT INTO categories (name, type, is_student_category, color)
VALUES ('Income', 'INCOME', false, '#4CAF50')
ON CONFLICT (name) DO NOTHING;
