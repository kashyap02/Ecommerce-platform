package com.ecommerce.orderservice.exception;

public class ProductServiceException extends RuntimeException {

    public ProductServiceException(String message) {
        super(message);
    }
}