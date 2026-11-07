package com.backendlearninglab.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility for managing database connectivity in OrderFlow.
 * <p>
 * TEACHING FOCUS:
 * 1. Low-level DriverManager: Creates a new physical TCP connection every single time.
 * 2. High-performance HikariCP DataSource: Reuses pre-authenticated socket connections.
 * 3. H2 In-Memory DB Mode: For testing/learning without requiring a live PostgreSQL instance.
 */
public final class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
    private static final Properties props = new Properties();
    private static HikariDataSource pooledDataSource;

    // Static initialization block runs once when the class is loaded by the JVM
    static {
        loadProperties();
    }

    private ConnectionManager() {
        // Private constructor prevents instantiation of utility class
    }

    private static void loadProperties() {
        try (InputStream in = ConnectionManager.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                log.warn("application.properties not found on classpath! Using defaults.");
            }
        } catch (IOException e) {
            log.error("Failed to load application.properties", e);
        }
    }

    /**
     * METHOD 1: Pure Low-Level DriverManager (Educational / Non-Pooled).
     * <p>
     * HOW IT WORKS:
     * - Parses the JDBC URL (e.g., jdbc:postgresql://localhost:5432/orderflow).
     * - Locates the registered Driver (org.postgresql.Driver).
     * - Establishes a brand-new TCP socket to port 5432.
     * - Performs database SSL/auth handshakes.
     * <p>
     * PERFORMANCE DRAWBACK:
     * Each connection creation takes 15ms - 50ms. Under 1000 requests/sec,
     * this destroys backend throughput and exhausts DB server process limits.
     */
    public static Connection createDirectRawConnection() throws SQLException {
        String url = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/orderflow");
        String user = props.getProperty("db.user", "postgres");
        String password = props.getProperty("db.password", "postgres");

        return DriverManager.getConnection(url, user, password);
    }

    /**
     * METHOD 2: Production Pooled DataSource (HikariCP).
     * <p>
     * HOW IT WORKS:
     * - Maintains a warm pool of open, validated connections.
     * - Calling ds.getConnection() borrows a connection in ~0.001ms.
     * - Calling conn.close() does NOT terminate the TCP socket; it simply returns the
     *   connection back to the pool for reuse!
     */
    public static synchronized DataSource getDataSource() {
        if (pooledDataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url", "jdbc:postgresql://localhost:5432/orderflow"));
            config.setUsername(props.getProperty("db.user", "postgres"));
            config.setPassword(props.getProperty("db.password", "postgres"));
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maximumPoolSize", "10")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minimumIdle", "2")));
            config.setPoolName("OrderFlow-HikariPool");

            try {
                pooledDataSource = new HikariDataSource(config);
                log.info("HikariCP DataSource initialized successfully: {}", config.getPoolName());
            } catch (Exception e) {
                log.warn("Could not connect to PostgreSQL ({}), falling back to embedded H2 for self-contained learning.", e.getMessage());
                return createH2FallbackDataSource();
            }
        }
        return pooledDataSource;
    }

    /**
     * Creates an embedded in-memory H2 database with PostgreSQL compatibility mode.
     * Ensures learners can test JDBC repositories even without PostgreSQL installed!
     */
    public static synchronized DataSource createH2FallbackDataSource() {
        HikariConfig h2Config = new HikariConfig();
        h2Config.setJdbcUrl("jdbc:h2:mem:orderflow_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        h2Config.setUsername("sa");
        h2Config.setPassword("");
        h2Config.setMaximumPoolSize(5);
        h2Config.setPoolName("OrderFlow-H2-FallbackPool");
        return new HikariDataSource(h2Config);
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}
