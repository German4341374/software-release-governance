package com.portfolio.releasegovernance.domain;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String type, Object id) {
        super(type + " '" + id + "' was not found.");
    }
}
