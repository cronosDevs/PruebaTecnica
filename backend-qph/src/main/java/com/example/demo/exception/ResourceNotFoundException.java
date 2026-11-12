package com.example.demo.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RECURSO_NO_ENCONTRADO, message);
    }
}
