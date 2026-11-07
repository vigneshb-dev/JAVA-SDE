package com.backendlearninglab.spring.observability;

import com.backendlearninglab.spring.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller exposing Health Checks and Runtime Observability Metrics.
 * <p>
 * TEACHING FOCUS: Production Readiness & Container Health Probes:
 * <ul>
 *   <li><b>Liveness Probes:</b> Determines if the application process is alive or crashed.</li>
 *   <li><b>Readiness Probes:</b> Validates that downstream critical dependencies (e.g. PostgreSQL)
 *       are reachable before load balancers route user traffic to this instance.</li>
 *   <li><b>Memory Metrics:</b> Monitors JVM Heap utilization to preempt OutOfMemoryError (OOM).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
public class HealthAndMetricsController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public HealthAndMetricsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> getHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("profile", activeProfile);

        // Database Readiness Check
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            health.put("database", (result != null && result == 1) ? "UP" : "DOWN");
        } catch (Exception e) {
            health.put("database", "DOWN: " + e.getMessage());
            health.put("status", "DEGRADED");
        }

        // JVM Runtime Metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemoryMb = runtime.totalMemory() / (1024 * 1024);
        long freeMemoryMb = runtime.freeMemory() / (1024 * 1024);
        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);

        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("usedMb", totalMemoryMb - freeMemoryMb);
        memory.put("freeMb", freeMemoryMb);
        memory.put("totalAllocatedMb", totalMemoryMb);
        memory.put("maxHeapMb", maxMemoryMb);
        health.put("jvmMemory", memory);

        health.put("jvmUptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);

        return ApiResponse.ok("System Health Status", health);
    }
}
