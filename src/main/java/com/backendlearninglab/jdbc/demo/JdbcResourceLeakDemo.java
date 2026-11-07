package com.backendlearninglab.jdbc.demo;

import com.backendlearninglab.util.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * EDUCATIONAL DEMO: Database Resource Leaks vs. Modern Try-With-Resources.
 * <p>
 * This class demonstrates:
 * 1. How forgetting to close Connection / Statement / ResultSet exhausts file descriptors
 *    and connection pool slots, crashing production servers.
 * 2. The painful old-school try/finally nested block nightmare (pre-Java 7).
 * 3. The modern, elegant try-with-resources (AutoCloseable).
 */
public class JdbcResourceLeakDemo {

    private static final Logger log = LoggerFactory.getLogger(JdbcResourceLeakDemo.class);

    public static void main(String[] args) {
        System.out.println("=== JDBC RESOURCE MANAGEMENT & LEAK PREVENTION ===");

        DataSource ds = ConnectionManager.createH2FallbackDataSource();

        // 1. The Historical Anti-Pattern (Manual try/finally with high leak risk)
        demonstrateHistoricalTryFinallyPattern(ds);

        // 2. The Professional Standard (try-with-resources)
        demonstrateProfessionalTryWithResources(ds);
    }

    /**
     * HISTORICAL PATTERN (Java 1.0 to Java 6):
     * <p>
     * WHAT'S WRONG HERE:
     * - Requires 20+ lines of noisy cleanup boilerplate.
     * - If conn.close() throws an exception, rs.close() or stmt.close() might never execute!
     * - Easy for junior engineers to forget the finally block entirely during bug fixes.
     */
    private static void demonstrateHistoricalTryFinallyPattern(DataSource ds) {
        System.out.println("\n[1] Old-school Manual Cleanup (Error-Prone):");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = ds.getConnection();
            stmt = conn.prepareStatement("SELECT 1 AS test_val");
            rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("  Executed query, result: " + rs.getInt("test_val"));
            }
        } catch (SQLException e) {
            log.error("Query failed", e);
        } finally {
            // NIGHTMARE: Each resource close can throw an SQLException itself!
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.warn("Error closing ResultSet", e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    log.warn("Error closing Statement", e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.warn("Error closing Connection", e);
                }
            }
            System.out.println("  Cleaned up all 3 handles manually in finally.");
        }
    }

    /**
     * MODERN PROFESSIONAL PATTERN: try-with-resources (Java 7+ AutoCloseable)
     * <p>
     * WHY THIS IS SUPERIOR:
     * 1. Any object implementing {@link java.lang.AutoCloseable} declared inside try(...)
     *    is guaranteed to be closed when execution leaves the block, even if an exception
     *    or return statement occurs.
     * 2. Resources are closed in REVERSE order of declaration:
     *    ResultSet is closed first, then PreparedStatement, then Connection.
     * 3. Suppressed Exceptions: If both the try block and close() throw an exception,
     *    the try-block exception is thrown, and the close() exception is attached as a
     *    suppressed exception (accessible via e.getSuppressed()), preventing error masking!
     */
    private static void demonstrateProfessionalTryWithResources(DataSource ds) {
        System.out.println("\n[2] Modern Try-With-Resources (Zero Leak Guarantee):");

        String sql = "SELECT 42 AS answer";

        // Reverse closure order: rs -> stmt -> conn automatically handled by the JVM
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                System.out.println("  Executed query safely, answer: " + rs.getInt("answer"));
            }

        } catch (SQLException e) {
            log.error("Database query failed", e);
        }

        System.out.println("  All resources automatically and safely closed by the JVM runtime.");
    }
}
