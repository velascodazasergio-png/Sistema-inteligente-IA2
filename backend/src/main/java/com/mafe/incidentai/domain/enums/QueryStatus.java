package com.mafe.incidentai.domain.enums;

// Estado de trazabilidad de la consulta enviada al flujo de IA en n8n
public enum QueryStatus {
    PENDING,
    COMPLETED,
    // n8n respondio, pero no encontro documentos relevantes en la base de conocimiento
    NO_RESULTS,
    // El backend no pudo entregar la consulta a n8n (timeout o error HTTP)
    COMMUNICATION_ERROR,
    // n8n recibio la consulta pero fallo al procesarla (embeddings, Qdrant o LLM)
    FAILED
}
