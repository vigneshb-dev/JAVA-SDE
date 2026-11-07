package com.backendlearninglab.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-Safe Configuration Properties.
 * <p>
 * WHY @ConfigurationProperties INSTEAD OF @Value("${...}")?
 * 1. Type-safety and IDE auto-completion.
 * 2. Hierarchical structure (e.g. inventory.lowStockThreshold).
 * 3. Supports Jakarta Bean Validation (@NotNull, @Min) on properties at startup.
 * 4. Eliminates repetitive string literals scattered across 50 classes.
 */
@ConfigurationProperties(prefix = "orderflow")
public class OrderFlowProperties {

    private String storeName = "OrderFlow Default";
    private String defaultCurrency = "USD";
    private int maxItemsPerOrder = 50;
    private final Inventory inventory = new Inventory();

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public int getMaxItemsPerOrder() {
        return maxItemsPerOrder;
    }

    public void setMaxItemsPerOrder(int maxItemsPerOrder) {
        this.maxItemsPerOrder = maxItemsPerOrder;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public static class Inventory {
        private int lowStockThreshold = 5;

        public int getLowStockThreshold() {
            return lowStockThreshold;
        }

        public void setLowStockThreshold(int lowStockThreshold) {
            this.lowStockThreshold = lowStockThreshold;
        }
    }
}
