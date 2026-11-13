package com.mafe.incidentai;

import com.mafe.incidentai.service.AiFailureHints;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// El detalle tecnico de n8n se traduce en una indicacion clara de que revisar
class AiFailureHintsTest {

    @Test
    void invalidApiKeyPointsToTheN8nCredential() {
        assertThat(AiFailureHints.hint("EMBEDDING_FALLO", "401 - \"{\\\"error\\\":{\\\"message\\\":\\\"User not found.\\\"}}\""))
                .contains("rechazó la clave").contains("OpenRouter API Key");
    }

    @Test
    void recognisesCreditsRateLimitsAndUnknownModels() {
        assertThat(AiFailureHints.hint("LLM_FALLO", "402 - insufficient credits")).contains("créditos");
        assertThat(AiFailureHints.hint("EMBEDDING_FALLO", "429 - rate limited")).contains("429");
        assertThat(AiFailureHints.hint("LLM_FALLO", "400 - \"model not found\"")).contains("LLM_MODEL");
    }

    @Test
    void recognisesConfigurationAndTimeouts() {
        assertThat(AiFailureHints.hint("CONFIGURACION_INVALIDA", "CONFIG: dimensión 768 distinta de EMBEDDING_DIM=1536"))
                .contains("EMBEDDING_DIM");
        assertThat(AiFailureHints.hint("TIMEOUT_QDRANT_NO_DISPONIBLE", "timeout of 15000ms exceeded")).contains("Qdrant");
    }

    @Test
    void returnsNothingWhenThereIsNoUsefulDetail() {
        assertThat(AiFailureHints.hint("EMBEDDING_FALLO", null)).isNull();
        assertThat(AiFailureHints.hint("SOLICITUD_INVALIDA", "detailedDescription está vacía")).isNull();
    }
}
