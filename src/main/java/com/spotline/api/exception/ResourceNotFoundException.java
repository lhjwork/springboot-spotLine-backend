package com.spotline.api.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(resourceName + "을(를) 찾을 수 없습니다: " + identifier);
    }
}
