package com.financetracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * FinanceTrackerApplicationTests.java — Basic application smoke test.
 *
 * PURPOSE: Verifies that the Spring application context loads without errors.
 * If any bean is misconfigured, any dependency is missing, or any annotation
 * is wrong, this test fails — giving you immediate feedback.
 *
 * This is the minimum test every Spring Boot app should have.
 * More specific tests (service tests, API tests) will be added per feature.
 *
 * @SpringBootTest — loads the full Spring context (all beans, all config)
 * @ActiveProfiles("test") — uses application-test.properties if it exists,
 *   so tests can use an in-memory H2 database instead of real PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
class FinanceTrackerApplicationTests {

    /**
     * contextLoads() — the test that verifies the app can start.
     * It has no assertions — if the Spring context fails to load,
     * the test itself throws an exception and fails.
     */
    @Test
    void contextLoads() {
        // If we get here, the application context started successfully.
        // All @Autowired dependencies were resolved, all config was valid.
    }
}
