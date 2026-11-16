package com.mafe.incidentai.exception;

// Custom exception thrown when an operation breaks a business rule (HTTP 409)
public class BusinessRuleException extends RuntimeException {

    // Creates the exception with the message returned to the client
    public BusinessRuleException(String message) {
        super(message);
    }
}

