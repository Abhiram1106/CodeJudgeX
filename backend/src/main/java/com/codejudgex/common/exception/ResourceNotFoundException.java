package com.codejudgex.common.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, UUID id) {
        super(resourceName + " not found: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
