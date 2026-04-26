package com.ecommerce.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /*
     * @Version enables optimistic locking.
     * When two concurrent requests try to update the same product row,
     * JPA checks that the version in the DB still matches the one read.
     * The loser gets an OptimisticLockException instead of a silent
     * overwrite — critical for stock correctness under concurrent orders.
     */
    @Version
    private Long version;

    @PrePersist
    protected  void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── Derived helper ────────────────────────────────────────────────────────

    /**
     * Stock that is actually available for new orders.
     * availableQuantity = stockQuantity − reservedQuantity
     */
    public int getAvailableQuantity() {
        return stockQuantity - reservedQuantity;
    }
}