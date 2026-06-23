package com.financetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is where my finance tracker app starts.
 * @SpringBootApplication does three things at once:
 *   - scans my packages for Spring components (controllers, services, repos)
 *   - enables auto-configuration so Spring Boot sets up what it detects
 *   - marks this as a configuration class
 *
 * Running main() starts an embedded Tomcat server on port 8080,
 * connects to my PostgreSQL database using application.properties,
 * creates the DB tables from my entity classes if they don't exist yet,
 * and registers all my REST controllers to handle incoming requests.
 */
@SpringBootApplication
public class FinanceTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceTrackerApplication.class, args);
    }
}
