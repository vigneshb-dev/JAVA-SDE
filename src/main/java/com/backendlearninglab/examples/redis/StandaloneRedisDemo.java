package com.backendlearninglab.examples.redis;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EDUCATIONAL DEMO: Redis In-Memory Key-Value Architecture Simulation.
 * <p>
 * This class demonstrates:
 * 1. How an in-memory key-value store operates in RAM (sub-microsecond read/write).
 * 2. TTL (Time-To-Live) and passive/active expiration mechanics.
 * 3. The LRU (Least Recently Used) cache eviction algorithm when RAM capacity is reached.
 * 4. Fundamental Redis Commands: SET, GET, DEL, EXPIRE, TTL.
 */
public class StandaloneRedisDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== STANDALONE REDIS KEY-VALUE & TTL SIMULATION ===");

        MiniRedisServer redis = new MiniRedisServer(3); // Small capacity of 3 to demonstrate eviction

        // 1. SET & GET
        System.out.println("\n[1] Basic Operations: SET and GET");
        redis.set("product:101:name", "Mechanical Keyboard Pro");
        redis.set("product:101:price", "129.99");
        System.out.println("  GET product:101:name  -> " + redis.get("product:101:name"));
        System.out.println("  GET product:101:price -> " + redis.get("product:101:price"));

        // 2. EXPIRE & TTL (Time To Live)
        System.out.println("\n[2] Setting TTL (Expiration):");
        redis.set("flash_sale_coupon", "50_PERCENT_OFF");
        redis.expire("flash_sale_coupon", 1); // 1-second TTL
        System.out.println("  Initial TTL of flash_sale_coupon: " + redis.ttl("flash_sale_coupon") + " seconds");
        System.out.println("  Value before expiration: " + redis.get("flash_sale_coupon"));

        System.out.println("  Sleeping 1100ms for TTL to expire...");
        Thread.sleep(1100);

        System.out.println("  Value after expiration: " + redis.get("flash_sale_coupon") + " (Expired and purged!)");
        System.out.println("  TTL after expiration: " + redis.ttl("flash_sale_coupon") + " (-2 = key does not exist)");

        // 3. LRU Eviction Simulation
        System.out.println("\n[3] Memory Eviction Policy: LRU (Least Recently Used):");
        redis.set("item:A", "Value A");
        redis.set("item:B", "Value B");
        redis.set("item:C", "Value C");
        System.out.println("  Cache currently holds: A, B, C");

        // Access item:A to make it recently used
        redis.get("item:A");
        System.out.println("  Accessed item:A (item:B is now the Least Recently Used)");

        // Insert item:D -> Capacity of 3 exceeded -> Evicts item:B!
        System.out.println("  Inserting item:D (forces eviction)...");
        redis.set("item:D", "Value D");

        System.out.println("  GET item:A -> " + redis.get("item:A") + " (Retained)");
        System.out.println("  GET item:B -> " + redis.get("item:B") + " (EVICTED by LRU policy!)");
        System.out.println("  GET item:C -> " + redis.get("item:C") + " (Retained)");
        System.out.println("  GET item:D -> " + redis.get("item:D") + " (Retained)");
    }

    /**
     * Micro In-Memory Key-Value Server with TTL and LRU Eviction.
     */
    static class MiniRedisServer {
        private final int maxCapacity;

        // LinkedHashMap with accessOrder=true implements exact LRU order!
        private final Map<String, CacheEntry> store;

        public MiniRedisServer(int maxCapacity) {
            this.maxCapacity = maxCapacity;
            this.store = new LinkedHashMap<>(maxCapacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                    return size() > maxCapacity;
                }
            };
        }

        public synchronized void set(String key, String value) {
            store.put(key, new CacheEntry(value, -1));
        }

        public synchronized String get(String key) {
            CacheEntry entry = store.get(key);
            if (entry == null) return null;

            // Check passive TTL expiration
            if (entry.isExpired()) {
                store.remove(key);
                return null;
            }
            return entry.value;
        }

        public synchronized void del(String key) {
            store.remove(key);
        }

        public synchronized boolean expire(String key, long seconds) {
            CacheEntry entry = store.get(key);
            if (entry == null || entry.isExpired()) return false;
            entry.expireAtMillis = System.currentTimeMillis() + (seconds * 1000);
            return true;
        }

        public synchronized long ttl(String key) {
            CacheEntry entry = store.get(key);
            if (entry == null) return -2; // Redis standard: -2 means key does not exist
            if (entry.isExpired()) {
                store.remove(key);
                return -2;
            }
            if (entry.expireAtMillis == -1) return -1; // Redis standard: -1 means key has no expiration
            return Math.max(0, (entry.expireAtMillis - System.currentTimeMillis()) / 1000);
        }

        private static class CacheEntry {
            final String value;
            long expireAtMillis;

            CacheEntry(String value, long expireAtMillis) {
                this.value = value;
                this.expireAtMillis = expireAtMillis;
            }

            boolean isExpired() {
                return expireAtMillis != -1 && System.currentTimeMillis() > expireAtMillis;
            }
        }
    }
}
