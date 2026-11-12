package com.mafe.incidentai.domain.enums;

// Estado de trazabilidad de la consulta enviada al flujo de IA en n8n
public enum QueryStatus {
    PENDING,
    COMPLETED,
    // n8n respondio, pero no encontro documentos relevantes en la base de conocimiento
    NO_RESULTS,
    COMMUNICATION_ERROR
}

