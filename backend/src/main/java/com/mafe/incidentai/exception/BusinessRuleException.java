package com.mafe.incidentai.exception;

// Custom exception thrown when an operation breaks a business rule (HTTP 409)
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

