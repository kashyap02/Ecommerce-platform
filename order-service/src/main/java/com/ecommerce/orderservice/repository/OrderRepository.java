package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Spring Data JPA auto-generates:
    // SELECT * FROM orders WHERE user_email = ?
    List<Order> findByUserEmail(String userEmail);
}