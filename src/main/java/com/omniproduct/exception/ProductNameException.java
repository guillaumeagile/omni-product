package com.omniproduct.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
public class ProductNameException extends RuntimeException {
    public ProductNameException(String message) {
        super(message);
    }
}
