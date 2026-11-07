package com.backendlearninglab.spring.controller;

import com.backendlearninglab.spring.dto.request.UpdateInventoryRequest;
import com.backendlearninglab.spring.dto.response.ApiResponse;
import com.backendlearninglab.spring.dto.response.InventoryResponse;
import com.backendlearninglab.spring.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<List<InventoryResponse>> getAllInventory() {
        return ApiResponse.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/{productId}")
    public ApiResponse<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        return ApiResponse.ok(inventoryService.getInventoryByProductId(productId));
    }

    /**
     * PATCH /api/v1/inventory/{productId}
     * Demonstrates @PatchMapping with a JSON request body for selective stock updates.
     */
    @PatchMapping("/{productId}")
    public ApiResponse<InventoryResponse> updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        InventoryResponse response = inventoryService.updateStock(productId, request);
        return ApiResponse.ok("Stock updated successfully", response);
    }
}
