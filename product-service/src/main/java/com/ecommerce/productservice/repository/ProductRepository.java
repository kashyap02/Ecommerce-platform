package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // JpaRepository already gives us:
    // save(), findById(), findAll(Pageable), deleteById(), existsById()
    // No custom queries needed for now - all stock logic lives in the service layer
}
