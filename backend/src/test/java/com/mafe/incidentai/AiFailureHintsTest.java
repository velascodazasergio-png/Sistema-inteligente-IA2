package com.mafe.incidentai;

import com.mafe.incidentai.service.AiFailureHints;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// The technical detail from n8n becomes a clear hint about what to check
class AiFailureHintsTest {

    @Test
    void invalidApiKeyPointsToTheN8nCredential() {
        assertThat(AiFailureHints.hint("EMBEDDING_FALLO", "401 - \"{\\\"error\\\":{\\\"message\\\":\\\"User not found.\\\"}}\""))
                .contains("rejected the API key").contains("OPENROUTER_API_KEY");
    }

    @Test
    void recognisesCreditsRateLimitsAndUnknownModels() {
        assertThat(AiFailureHints.hint("LLM_FALLO", "402 - insufficient credits")).contains("credits");
        assertThat(AiFailureHints.hint("EMBEDDING_FALLO", "429 - rate limited")).contains("429");
        assertThat(AiFailureHints.hint("LLM_FALLO", "400 - \"model not found\"")).contains("LLM_MODEL");
    }

    @Test
    void recognisesConfigurationAndTimeouts() {
        assertThat(AiFailureHints.hint("CONFIGURACION_INVALIDA", "CONFIG: dimension 768 differs from EMBEDDING_DIM=1536"))
                .contains("EMBEDDING_DIM");
        assertThat(AiFailureHints.hint("TIMEOUT_QDRANT_NO_DISPONIBLE", "timeout of 15000ms exceeded")).contains("Qdrant");
        assertThat(AiFailureHints.hint("QDRANT_NO_DISPONIBLE", "socket hang up")).contains("connection between n8n and Qdrant was cut");
    }

    // The frontend translates these exact sentences (frontend/tests/failure-hints.test.mjs uses the same ones)
    @Test
    void hintsKeepTheWordingTheFrontendTranslates() {
        assertThat(AiFailureHints.hint("CONFIGURACION_INVALIDA", "CONFIG: dimension 768"))
                .isEqualTo("The embedding model dimension does not match EMBEDDING_DIM: check the .env file.");
        assertThat(AiFailureHints.hint("CONFIGURACION_INVALIDA", "CONFIG: LLM_MODEL"))
                .isEqualTo("An n8n configuration variable is missing (EMBEDDING_MODEL, LLM_MODEL or RAG_SCORE_THRESHOLD): check the .env file.");
        assertThat(AiFailureHints.hint("EMBEDDING_FALLO", "401 - unauthorized"))
                .isEqualTo("OpenRouter (embedding model) rejected the API key: check OPENROUTER_API_KEY in the .env file"
                        + " and apply the change with \"docker compose up -d n8n\".");
        assertThat(AiFailureHints.hint("LLM_FALLO", "402 - insufficient credits"))
                .isEqualTo("The OpenRouter account does not have enough credits.");
        assertThat(AiFailureHints.hint("LLM_FALLO", "429 - rate limited"))
                .isEqualTo("OpenRouter (chat model) rate-limited the requests (429): wait a moment and try again.");
        assertThat(AiFailureHints.hint("LLM_FALLO", "404 - model not found"))
                .isEqualTo("OpenRouter (chat model) does not recognize the configured model: check EMBEDDING_MODEL or LLM_MODEL in the .env file.");
        assertThat(AiFailureHints.hint("QDRANT_NO_DISPONIBLE", "404 - collection not found"))
                .isEqualTo("The Qdrant collection does not exist: run the initial indexing of workflow 01.");
        assertThat(AiFailureHints.hint("QDRANT_NO_DISPONIBLE", "ETIMEDOUT"))
                .isEqualTo("Qdrant took too long to respond: try again.");
        assertThat(AiFailureHints.hint("QDRANT_NO_DISPONIBLE", "ECONNRESET"))
                .isEqualTo("The connection between n8n and Qdrant was cut: try again.");
        assertThat(AiFailureHints.hint("EMBEDDING_FALLO", "ECONNREFUSED"))
                .isEqualTo("n8n could not connect to OpenRouter (embedding model): check the network or the configured URL.");
    }

    @Test
    void returnsNothingWhenThereIsNoUsefulDetail() {
        assertThat(AiFailureHints.hint("EMBEDDING_FALLO", null)).isNull();
        assertThat(AiFailureHints.hint("SOLICITUD_INVALIDA", "detailedDescription is empty")).isNull();
    }
}
