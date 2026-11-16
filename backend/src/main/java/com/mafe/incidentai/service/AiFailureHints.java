package com.mafe.incidentai.service;

import java.util.Locale;

// Turns the technical detail sent by n8n (HTTP response from OpenRouter or Qdrant, timeouts, configuration
// errors) into a short hint about what to check. The raw detail is never stored or shown: it can contain
// internal data. The frontend translates these hints into the language of the interface
// (frontend/js/api-messages.js), so their wording is part of the API contract.
public final class AiFailureHints {

    // Utility class: not meant to be instantiated
    private AiFailureHints() {
    }

    // Hint about what to check for an n8n failure, or null if the detail says nothing useful
    public static String hint(String errorCode, String technicalDetail) {
        String code = errorCode == null ? "" : errorCode.toUpperCase(Locale.ROOT);
        String detail = technicalDetail == null ? "" : technicalDetail.toLowerCase(Locale.ROOT);
        String service = code.contains("QDRANT") ? "Qdrant"
                : code.contains("EMBEDDING") ? "OpenRouter (embedding model)"
                : code.contains("LLM") ? "OpenRouter (chat model)"
                : null;

        if (detail.isBlank()) {
            return null;
        }
        if (detail.contains("config:") && detail.contains("dimens")) {
            return "The embedding model dimension does not match EMBEDDING_DIM: check the .env file.";
        }
        if (detail.contains("config:")) {
            return "An n8n configuration variable is missing (EMBEDDING_MODEL, LLM_MODEL or RAG_SCORE_THRESHOLD): check the .env file.";
        }
        if (service == null) {
            return null;
        }
        if (startsWithStatus(detail, "401") || startsWithStatus(detail, "403")) {
            return service + " rejected the API key: check OPENROUTER_API_KEY in the .env file"
                    + " and apply the change with \"docker compose up -d n8n\".";
        }
        if (startsWithStatus(detail, "402")) {
            return "The OpenRouter account does not have enough credits.";
        }
        if (startsWithStatus(detail, "429")) {
            return service + " rate-limited the requests (429): wait a moment and try again.";
        }
        // "model" also matches the Spanish "modelo" that some n8n messages contain
        if ((startsWithStatus(detail, "400") || startsWithStatus(detail, "404")) && detail.contains("model")) {
            return service + " does not recognize the configured model: check EMBEDDING_MODEL or LLM_MODEL in the .env file.";
        }
        if (startsWithStatus(detail, "404") && service.equals("Qdrant")) {
            return "The Qdrant collection does not exist: run the initial indexing of workflow 01.";
        }
        if (detail.contains("timeout") || detail.contains("timed out") || detail.contains("etimedout")) {
            return service + " took too long to respond: try again.";
        }
        if (detail.contains("socket hang up") || detail.contains("econnreset")) {
            return "The connection between n8n and " + service + " was cut: try again.";
        }
        if (detail.contains("econnrefused") || detail.contains("enotfound") || detail.contains("eai_again")) {
            return "n8n could not connect to " + service + ": check the network or the configured URL.";
        }
        return null;
    }

    // n8n writes HTTP errors as "401 - {...}"
    private static boolean startsWithStatus(String detail, String status) {
        return detail.startsWith(status + " ") || detail.startsWith(status + "-") || detail.equals(status);
    }
}
