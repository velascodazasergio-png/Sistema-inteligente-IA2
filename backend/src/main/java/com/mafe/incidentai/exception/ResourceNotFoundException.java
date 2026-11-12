package com.mafe.incidentai.exception;

// Excepcion personalizada que se lanza cuando no se encuentra un recurso en la base de datos (HTTP 404)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

