package com.mafe.incidentai.client;

import com.mafe.incidentai.dto.AiAssistanceRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

// Componente cliente encargado de enviar las peticiones HTTP POST al webhook de n8n
@Component
@RequiredArgsConstructor
@Slf4j
public class N8nWebhookClient {

    private final RestTemplate restTemplate;

    @Value("${n8n.webhook.assistance-url}")
    private String n8nWebhookUrl;

    // Autenticacion del webhook (credencial "Header Auth" en n8n). Si el valor esta vacio no se envia.
    @Value("${n8n.webhook.auth-header-name:X-N8N-Token}")
    private String authHeaderName;

    @Value("${n8n.webhook.auth-header-value:}")
    private String authHeaderValue;

    // Webhook del flujo 01 (indexacion). Vacio = la reindexacion automatica esta desactivada.
    @Value("${n8n.webhook.index-url:}")
    private String indexWebhookUrl;

    // Dispara el webhook de n8n enviando el ID de la consulta IA y los datos del incidente; retorna true si n8n respondio 2xx
    public boolean triggerIncidentAnalysis(Long aiQueryId, AiAssistanceRequestDTO request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeaderValue != null && !authHeaderValue.isBlank()) {
                headers.set(authHeaderName, authHeaderValue);
            }

            // Construimos el payload JSON incluyendo el aiQueryId para que n8n sepa a que consulta asociar su respuesta
            Map<String, Object> payload = new HashMap<>();
            payload.put("aiQueryId", aiQueryId);
            payload.put("incidentId", request.incidentId());
            payload.put("incidentTitle", request.incidentTitle());
            payload.put("detailedDescription", request.detailedDescription());
            payload.put("categoryName", request.categoryName());
            payload.put("priority", request.priority());
            payload.put("additionalPromptContext", request.additionalPromptContext());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.info("Sending AI analysis request to n8n webhook for aiQueryId={} and incidentId={}", aiQueryId, request.incidentId());
            restTemplate.postForEntity(n8nWebhookUrl, entity, String.class);
            return true;

        } catch (RestClientException ex) {
            // Capturamos timeouts (ResourceAccessException) o errores HTTP 4xx/5xx de n8n sin romper la transaccion principal
            log.error("Failed to communicate with n8n webhook for aiQueryId={}: {}", aiQueryId, ex.getMessage());
            return false;
        }
    }

    // Pide al flujo 01 que reindexe un articulo. Nunca lanza excepciones: si falla, el articulo se
    // puede reindexar despues llamando al mismo webhook (o con {"all": true} para todos).
    public void triggerKnowledgeIndexing(Long articleId) {
        if (indexWebhookUrl == null || indexWebhookUrl.isBlank()) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeaderValue != null && !authHeaderValue.isBlank()) {
                headers.set(authHeaderName, authHeaderValue);
            }
            restTemplate.postForEntity(indexWebhookUrl, new HttpEntity<>(Map.of("articleId", articleId), headers), String.class);
            log.info("Knowledge article {} sent to the n8n indexing workflow", articleId);
        } catch (RestClientException ex) {
            log.warn("Could not reindex knowledge article {} in n8n: {}", articleId, ex.getMessage());
        }
    }
}
