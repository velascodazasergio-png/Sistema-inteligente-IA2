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

// Client component that sends the HTTP POST requests to the n8n webhooks
@Component
@RequiredArgsConstructor
@Slf4j
public class N8nWebhookClient {

    private final RestTemplate restTemplate;

    @Value("${n8n.webhook.assistance-url}")
    private String n8nWebhookUrl;

    // Webhook authentication ("Header Auth" credential in n8n). When the value is empty, no header is sent.
    @Value("${n8n.webhook.auth-header-name:X-N8N-Token}")
    private String authHeaderName;

    @Value("${n8n.webhook.auth-header-value:}")
    private String authHeaderValue;

    // Webhook of workflow 01 (indexing). Empty = automatic reindexing is disabled.
    @Value("${n8n.webhook.index-url:}")
    private String indexWebhookUrl;

    // Calls the n8n webhook with the AI query ID and the incident data; returns true if n8n answered 2xx
    public boolean triggerIncidentAnalysis(Long aiQueryId, AiAssistanceRequestDTO request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeaderValue != null && !authHeaderValue.isBlank()) {
                headers.set(authHeaderName, authHeaderValue);
            }

            // The JSON payload includes the aiQueryId so n8n knows which query its answer belongs to
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
            // Catches timeouts (ResourceAccessException) and HTTP 4xx/5xx errors from n8n without breaking the main transaction
            log.error("Failed to communicate with n8n webhook for aiQueryId={}: {}", aiQueryId, ex.getMessage());
            return false;
        }
    }

    // Asks workflow 01 to reindex an article. Never throws: if it fails, the article can be
    // reindexed later by calling the same webhook (or with {"all": true} for every article).
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
