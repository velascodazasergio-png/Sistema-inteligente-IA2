package com.mafe.incidentai.exception;

// Custom exception thrown when a resource is not found in the database (HTTP 404)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

