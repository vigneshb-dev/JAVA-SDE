package com.backendlearninglab.spring.service;

import com.backendlearninglab.spring.dto.request.UpdateInventoryRequest;
import com.backendlearninglab.spring.dto.response.InventoryResponse;
import com.backendlearninglab.spring.entity.InventoryEntity;
import com.backendlearninglab.spring.exception.ResourceNotFoundException;
import com.backendlearninglab.spring.repository.SpringDataInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final SpringDataInventoryRepository inventoryRepository;

    public InventoryService(SpringDataInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryResponse getInventoryByProductId(Long productId) {
        return inventoryRepository.findById(productId)
                .map(InventoryResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory for Product", productId));
    }

    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(InventoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public InventoryResponse updateStock(Long productId, UpdateInventoryRequest request) {
        InventoryEntity inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory for Product", productId));

        if (request.availableStock() != null) {
            inventory.setAvailableStock(request.availableStock());
            inventory.setLastRestockedAt(Instant.now());
        }
        if (request.warehouseLocation() != null) {
            inventory.setWarehouseLocation(request.warehouseLocation());
        }

        // Dirty checking automatically syncs updates on transaction completion
        return InventoryResponse.fromEntity(inventory);
    }
}
