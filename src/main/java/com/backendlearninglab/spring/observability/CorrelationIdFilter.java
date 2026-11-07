package com.backendlearninglab.spring.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Observability Filter establishing Distributed Tracing via Correlation IDs.
 * <p>
 * TEACHING FOCUS: Request Tracing & Structured Logging:
 * <ul>
 *   <li><b>WHY THIS EXISTS:</b> In a microservice ecosystem handling 10,000 requests per second,
 *       logs from different concurrent users are interleaved. A Correlation ID (Trace ID) binds
 *       all log statements produced during a single HTTP request together.</li>
 *   <li><b>MDC (Mapped Diagnostic Context):</b> Backed by {@link ThreadLocal}, SLF4J injects
 *       the correlation ID into every log message automatically without passing it as a method parameter.</li>
 *   <li><b>CRITICAL RULE:</b> Always clean up MDC in a {@code finally} block to prevent ID leakage
 *       when threads return to Tomcat's worker thread pool!</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        try {
            MDC.put(MDC_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
