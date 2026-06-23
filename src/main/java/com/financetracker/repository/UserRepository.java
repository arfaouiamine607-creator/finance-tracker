package com.financetracker.repository;

import com.financetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository.java — Data access layer for the User entity.
 *
 * This interface handles all queries to the "users" table in PostgreSQL.
 * Extending JpaRepository gives all CRUD operations for free:
 *   - save(user)          → INSERT or UPDATE
 *   - findById(id)        → SELECT WHERE id = ?
 *   - findAll()           → SELECT *
 *   - delete(user)        → DELETE WHERE id = ?
 *   - count()             → SELECT COUNT(*)
 *   ... and many more
 *
 * I only add methods that JpaRepository doesn't already provide.
 * Spring Data JPA reads the method NAMES and generates the SQL automatically.
 * "findByEmail" → "SELECT * FROM users WHERE email = ?"
 *
 * @Repository tells Spring this is a data access component.
 * JpaRepository<User, Long>:
 *   - User = the entity class this repo manages
 *   - Long = the type of the primary key (id)
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     * Used by Spring Security during login to load the user.
     * Returns Optional<User> — empty if no user has that email (not found).
     *
     * Spring generates: SELECT * FROM users WHERE email = ? LIMIT 1
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their username.
     * Used for username-availability checks during registration.
     *
     * Spring generates: SELECT * FROM users WHERE username = ? LIMIT 1
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if an email is already registered.
     * More efficient than findByEmail() when only a boolean check is needed.
     *
     * Spring generates: SELECT COUNT(*) > 0 FROM users WHERE email = ?
     */
    boolean existsByEmail(String email);

    /**
     * Check if a username is already taken.
     * Used during registration to reject duplicate usernames immediately.
     *
     * Spring generates: SELECT COUNT(*) > 0 FROM users WHERE username = ?
     */
    boolean existsByUsername(String username);
}
