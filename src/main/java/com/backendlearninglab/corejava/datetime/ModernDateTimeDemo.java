package com.backendlearninglab.corejava.datetime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CORE JAVA: Modern Date/Time API (java.time) vs Legacy java.util.Date.
 * <p>
 * RUNNABLE: Contains its own main() method.
 */
public class ModernDateTimeDemo {

    public static void main(String[] args) {
        System.out.println("=== MODERN JAVA DATE/TIME API (java.time) ===");

        demonstrateInstantForDatabase();
        demonstrateZonedTimes();
        demonstrateDurationsAndPeriods();
        demonstrateThreadSafeFormatting();
    }

    /**
     * WHY java.util.Date WAS FLAWED:
     * 1. Mutable: Anyone could do date.setTime(1000) and mutate the object stored in another object!
     * 2. Months were 0-indexed (January was 0, December was 11) - a source of countless bugs.
     * 3. SimpleDateFormat was NOT THREAD-SAFE. Sharing it across threads corrupted dates.
     * <p>
     * THE SOLUTION: java.time (JSR-310)
     * All classes are completely IMMUTABLE and THREAD-SAFE.
     */
    private static void demonstrateInstantForDatabase() {
        System.out.println("\n[1] Instant: UTC Machine Time (The Standard for Database Timestamps):");

        Instant nowUtc = Instant.now();
        System.out.println("  Current UTC Instant: " + nowUtc);

        Instant orderShippedDeadline = nowUtc.plus(Duration.ofHours(24));
        System.out.println("  Order Must Ship By:  " + orderShippedDeadline);
    }

    private static void demonstrateZonedTimes() {
        System.out.println("\n[2] Handling Multiple Timezones in E-Commerce:");

        Instant orderTimestamp = Instant.now();

        // Customer in New York
        ZonedDateTime nyTime = orderTimestamp.atZone(ZoneId.of("America/New_York"));
        // Customer in Tokyo
        ZonedDateTime tokyoTime = orderTimestamp.atZone(ZoneId.of("Asia/Tokyo"));

        System.out.println("  Order placed (New York): " + nyTime);
        System.out.println("  Order placed (Tokyo):    " + tokyoTime);
    }

    private static void demonstrateDurationsAndPeriods() {
        System.out.println("\n[3] Durations (Time-based) vs. Periods (Date-based):");

        // Duration: Hours, minutes, seconds, nanoseconds
        Duration apiTimeout = Duration.ofMinutes(5);
        System.out.println("  API Timeout in seconds: " + apiTimeout.toSeconds() + "s");

        // Period: Years, months, days
        LocalDate birthDate = LocalDate.of(1995, 6, 15);
        LocalDate currentDate = LocalDate.now();
        Period age = Period.between(birthDate, currentDate);
        System.out.printf("  Customer account age: %d years, %d months, %d days%n",
                age.getYears(), age.getMonths(), age.getDays());
    }

    private static void demonstrateThreadSafeFormatting() {
        System.out.println("\n[4] Thread-Safe DateTimeFormatter:");

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String formatted = now.format(formatter);
        System.out.println("  Formatted invoice timestamp: " + formatted);

        LocalDateTime parsed = LocalDateTime.parse(formatted, formatter);
        System.out.println("  Successfully parsed back:   " + parsed);
    }
}
