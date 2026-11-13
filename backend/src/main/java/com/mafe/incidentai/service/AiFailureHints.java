package com.mafe.incidentai.service;

import java.util.Locale;

// Traduce el detalle tecnico que envia n8n (respuesta HTTP de OpenRouter o Qdrant, timeouts, errores de
// configuracion) a una indicacion corta en espanol sobre que revisar. El detalle crudo no se guarda ni se
// muestra: puede contener datos internos.
public final class AiFailureHints {

    private AiFailureHints() {
    }

    public static String hint(String errorCode, String technicalDetail) {
        String code = errorCode == null ? "" : errorCode.toUpperCase(Locale.ROOT);
        String detail = technicalDetail == null ? "" : technicalDetail.toLowerCase(Locale.ROOT);
        String service = code.contains("QDRANT") ? "Qdrant"
                : code.contains("EMBEDDING") ? "OpenRouter (modelo de embeddings)"
                : code.contains("LLM") ? "OpenRouter (modelo de chat)"
                : null;

        if (detail.isBlank()) {
            return null;
        }
        if (detail.contains("config:") && detail.contains("dimens")) {
            return "La dimensión del modelo de embeddings no coincide con EMBEDDING_DIM: revisa el .env.";
        }
        if (detail.contains("config:")) {
            return "Falta una variable de configuración de n8n (EMBEDDING_MODEL, LLM_MODEL o RAG_SCORE_THRESHOLD): revisa el .env.";
        }
        if (service == null) {
            return null;
        }
        if (startsWithStatus(detail, "401") || startsWithStatus(detail, "403")) {
            return service + " rechazó la clave de API: revisa la credencial \"OpenRouter API Key\" en n8n"
                    + " (debe ser \"Bearer <tu clave>\").";
        }
        if (startsWithStatus(detail, "402")) {
            return "La cuenta de OpenRouter no tiene créditos suficientes.";
        }
        if (startsWithStatus(detail, "429")) {
            return service + " limitó las solicitudes (429): espera un momento y vuelve a intentarlo.";
        }
        if ((startsWithStatus(detail, "400") || startsWithStatus(detail, "404"))
                && (detail.contains("model") || detail.contains("modelo"))) {
            return service + " no reconoce el modelo configurado: revisa EMBEDDING_MODEL o LLM_MODEL en el .env.";
        }
        if (startsWithStatus(detail, "404") && service.equals("Qdrant")) {
            return "No existe la colección en Qdrant: ejecuta la indexación inicial del flujo 01.";
        }
        if (detail.contains("timeout") || detail.contains("timed out") || detail.contains("etimedout")) {
            return service + " tardó demasiado en responder: vuelve a intentarlo.";
        }
        if (detail.contains("econnrefused") || detail.contains("enotfound") || detail.contains("eai_again")) {
            return "n8n no pudo conectarse con " + service + ": revisa la red o la URL configurada.";
        }
        return null;
    }

    // n8n escribe los errores HTTP como "401 - {...}"
    private static boolean startsWithStatus(String detail, String status) {
        return detail.startsWith(status + " ") || detail.startsWith(status + "-") || detail.equals(status);
    }
}
