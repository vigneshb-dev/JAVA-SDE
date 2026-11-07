package com.backendlearninglab.examples.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EDUCATIONAL SIMULATION: Manual REST HTTP Server (Without Spring MVC).
 * <p>
 * PURPOSE:
 * Uses Java's built-in `com.sun.net.httpserver.HttpServer` to reveal the massive amount
 * of low-level HTTP mechanics, socket I/O, routing, and header parsing that Spring MVC's
 * `DispatcherServlet` quietly abstracts away for you.
 */
public class ManualHttpServerDemo {

    private static final int PORT = 8089;

    public static void main(String[] args) throws IOException {
        System.out.println("=== EDUCATIONAL EXERCISE: MANUAL REST HTTP SERVER ===");

        // 1. Create raw TCP HTTP Server listening on port 8089
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(4)); // Worker thread pool

        // 2. Register low-level HTTP handler context
        server.createContext("/api/orders", new OrderHttpHandler());

        server.start();
        System.out.println("Manual HTTP Server running at http://localhost:" + PORT + "/api/orders");
        System.out.println("Supported Endpoints:");
        System.out.println("  - GET  /api/orders/{id}   (Manual Path Variable)");
        System.out.println("  - POST /api/orders        (Manual Request Body Reading)");
        System.out.println("Press Ctrl+C to terminate or send an HTTP request to observe.\n");
    }

    /**
     * SIMULATION OF SPRING MVC's DispatcherServlet & HandlerAdapter
     */
    static class OrderHttpHandler implements HttpHandler {

        private static final Pattern ORDER_BY_ID_PATTERN = Pattern.compile("^/api/orders/(\\d+)$");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            System.out.println("[ManualHttpServer] Received " + method + " " + path);

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGet(exchange, path);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handlePost(exchange);
                } else {
                    // 405 Method Not Allowed
                    sendJsonResponse(exchange, 405, "{\"error\": \"Method not allowed: " + method + "\"}");
                }
            } catch (Exception e) {
                // Manual 500 Internal Server Error fallback
                sendJsonResponse(exchange, 500, "{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
            }
        }

        /**
         * WHAT SPRING MVC DOES AUTOMATICALLY WITH:
         *   @GetMapping("/{id}")
         *   public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id)
         */
        private void handleGet(HttpExchange exchange, String path) throws IOException {
            Matcher matcher = ORDER_BY_ID_PATTERN.matcher(path);
            if (matcher.matches()) {
                // Manual path variable extraction and string-to-long type conversion!
                String idStr = matcher.group(1);
                long orderId = Long.parseLong(idStr);

                if (orderId == 101L) {
                    String json = "{\"id\": 101, \"orderNumber\": \"ORD-2026-001\", \"total\": 299.99, \"status\": \"PAID\"}";
                    sendJsonResponse(exchange, 200, json);
                } else {
                    sendJsonResponse(exchange, 404, "{\"error\": \"Order not found with id: " + orderId + "\"}");
                }
            } else {
                sendJsonResponse(exchange, 400, "{\"error\": \"Malformed URL path. Expected /api/orders/{id}\"}");
            }
        }

        /**
         * WHAT SPRING MVC DOES AUTOMATICALLY WITH:
         *   @PostMapping
         *   public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request)
         */
        private void handlePost(HttpExchange exchange) throws IOException {
            // Manual InputStream reading and buffer decoding
            InputStream requestBodyStream = exchange.getRequestBody();
            String rawJsonBody = new String(requestBodyStream.readAllBytes(), StandardCharsets.UTF_8);

            if (rawJsonBody.isBlank()) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Request body cannot be empty\"}");
                return;
            }

            System.out.println("  -> Read raw JSON payload: " + rawJsonBody);

            // In real Spring MVC, Jackson's ObjectMapper transforms raw JSON into typed POJOs,
            // and Bean Validation validates constraints (@NotNull, @Min) automatically.
            String createdResponseJson = "{\"id\": 202, \"status\": \"CREATED\", \"payloadReceived\": " + rawJsonBody + "}";
            sendJsonResponse(exchange, 201, createdResponseJson);
        }

        /**
         * Low-level HTTP response writer:
         * Sets HTTP Status, writes Content-Type headers, measures byte lengths, and flushes output streams.
         */
        private void sendJsonResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
                os.flush();
            }
        }
    }
}
