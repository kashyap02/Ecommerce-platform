package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.StockOperationRequest;
import com.ecommerce.productservice.dto.UpdateProductRequest;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    // ── Catalog operations ────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .reservedQuantity(0)
                .build();

        Product saved = productRepository.save(product);
        log.info("Created product id={} name={}", saved.getId(), saved.getName());
        return ProductResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return ProductResponse.from(findByIdOrThrow(id));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = findByIdOrThrow(id);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        Product updated = productRepository.save(product);
        log.info("Updated product id={}", updated.getId());
        return ProductResponse.from(updated);
    }

    // ── Stock operations ──────────────────────────────────────────────────────

    /*
     * @Transactional here is essential for optimistic locking correctness.
     * The read + write happen inside one transaction, so the @Version check
     * fires on commit. If two threads both read version=5, the first to commit
     * wins (version becomes 6); the second gets OptimisticLockException.
     * GlobalExceptionHandler maps that to HTTP 409 so the caller can retry.
     */

    @Override
    @Transactional
    public void reserveStock(Long productId, StockOperationRequest request) {
        Product product = findByIdOrThrow(productId);

        int available = product.getAvailableQuantity();
        int requested = request.getQuantity();

        if (available < requested) {
            throw new InsufficientStockException(productId, requested, available);
        }

        product.setReservedQuantity(product.getReservedQuantity() + requested);
        productRepository.save(product);

        log.info("Reserved {} units for product id={}. Reserved now: {}, Available now: {}",
                requested, productId, product.getReservedQuantity(), product.getAvailableQuantity());
    }

    @Override
    @Transactional
    public void releaseStock(Long productId, StockOperationRequest request) {
        Product product = findByIdOrThrow(productId);

        int toRelease = request.getQuantity();
        // Guard: reservedQuantity should never go below 0
        int newReserved = Math.max(0, product.getReservedQuantity() - toRelease);
        product.setReservedQuantity(newReserved);
        productRepository.save(product);

        log.info("Released {} units for product id={}. Reserved now: {}",
                toRelease, productId, newReserved);
    }

    @Override
    @Transactional
    public void confirmStock(Long productId, StockOperationRequest request) {
        Product product = findByIdOrThrow(productId);

        int quantity = request.getQuantity();

        /*
         * On payment-confirmed:
         *   stockQuantity    -= quantity  (units physically shipped)
         *   reservedQuantity -= quantity  (reservation fulfilled)
         * Both move together to keep accounting consistent.
         */
        int newStock    = Math.max(0, product.getStockQuantity()    - quantity);
        int newReserved = Math.max(0, product.getReservedQuantity() - quantity);

        product.setStockQuantity(newStock);
        product.setReservedQuantity(newReserved);
        productRepository.save(product);

        log.info("Confirmed {} units for product id={}. Stock now: {}, Reserved now: {}",
                quantity, productId, newStock, newReserved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Product findByIdOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}