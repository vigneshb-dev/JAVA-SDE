package com.backendlearninglab.spring.controller;

import com.backendlearninglab.spring.dto.request.CreateProductRequest;
import com.backendlearninglab.spring.dto.response.ApiResponse;
import com.backendlearninglab.spring.dto.response.ProductResponse;
import com.backendlearninglab.spring.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ApiResponse.ok("Product created successfully", response);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProductById(id));
    }

    /**
     * GET /api/v1/products
     * GET /api/v1/products?category=ELECTRONICS
     */
    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts(@RequestParam(required = false) String category) {
        List<ProductResponse> products = (category != null && !category.isBlank())
                ? productService.getProductsByCategory(category)
                : productService.getAllActiveProducts();
        return ApiResponse.ok(products);
    }

    /**
     * PUT /api/v1/products/{id}/price?newPrice=199.99
     * HTTP PUT: Used for replacing or setting a complete field/resource state.
     */
    @PutMapping("/{id}/price")
    public ApiResponse<ProductResponse> updateProductPrice(
            @PathVariable Long id,
            @RequestParam BigDecimal newPrice) {
        ProductResponse updated = productService.updatePrice(id, newPrice);
        return ApiResponse.ok("Product price updated", updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateProduct(@PathVariable Long id) {
        productService.deactivateProduct(id);
    }
}
