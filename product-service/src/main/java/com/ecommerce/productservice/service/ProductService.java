package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.StockOperationRequest;
import com.ecommerce.productservice.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    // ── Catalog operations ────────────────────────────────────────────────────

    ProductResponse createProduct(CreateProductRequest request);

    Page<ProductResponse> getAllProducts(Pageable pageable);

    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    // ── Stock operations (Saga hooks) ─────────────────────────────────────────

    /**
     * Reserve stock for a pending order.
     * Increments reservedQuantity by the requested amount.
     * Throws InsufficientStockException if available stock < requested.
     */
    void reserveStock(Long productId, StockOperationRequest request);

    /**
     * Release previously reserved stock on payment failure.
     * Decrements reservedQuantity by the requested amount.
     */
    void releaseStock(Long productId, StockOperationRequest request);

    /**
     * Confirm stock deduction on payment success.
     * Decrements both stockQuantity AND reservedQuantity by the requested amount.
     */
    void confirmStock(Long productId, StockOperationRequest request);
}
