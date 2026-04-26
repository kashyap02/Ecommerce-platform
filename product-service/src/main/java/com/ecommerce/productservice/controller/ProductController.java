package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.StockOperationRequest;
import com.ecommerce.productservice.dto.UpdateProductRequest;
import com.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ── Catalog endpoints ─────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {

        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    // ── Stock / Saga endpoints ────────────────────────────────────────────────

    /**
     * Called by Order Service before publishing order-placed event.
     * Locks stock so it cannot be sold to another order.
     */
    @PostMapping("/{id}/reserve")
    public ResponseEntity<Void> reserveStock(
            @PathVariable Long id,
            @Valid @RequestBody StockOperationRequest request) {

        productService.reserveStock(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Called by Order Service on payment-failed event.
     * Returns reserved stock to the available pool.
     */
    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseStock(
            @PathVariable Long id,
            @Valid @RequestBody StockOperationRequest request) {

        productService.releaseStock(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Called by Order Service on payment-confirmed event.
     * Permanently deducts stock — the order is fulfilled.
     * Decrements both stockQuantity and reservedQuantity.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmStock(
            @PathVariable Long id,
            @Valid @RequestBody StockOperationRequest request) {

        productService.confirmStock(id, request);
        return ResponseEntity.ok().build();
    }
}