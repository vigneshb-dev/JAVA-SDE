package com.backendlearninglab.spring.exception;

import com.backendlearninglab.exception.OrderFlowException;

public class ResourceNotFoundException extends OrderFlowException {

    private final String resourceName;
    private final Object identifier;

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s not found with identifier: %s", resourceName, identifier));
        this.resourceName = resourceName;
        this.identifier = identifier;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Object getIdentifier() {
        return identifier;
    }
}
