package com.mafe.incidentai.domain.enums;

// Tracking status of the query sent to the AI workflow in n8n
public enum QueryStatus {
    PENDING,
    COMPLETED,
    // n8n answered, but found no relevant documents in the knowledge base
    NO_RESULTS,
    // The backend could not deliver the query to n8n (timeout or HTTP error)
    COMMUNICATION_ERROR,
    // n8n received the query but failed to process it (embeddings, Qdrant or LLM)
    FAILED
}
