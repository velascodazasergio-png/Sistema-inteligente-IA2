package com.mafe.incidentai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

// Configuracion del cliente HTTP RestTemplate con tiempos maximos de espera (timeouts) hacia n8n
@Configuration
public class RestClientConfig {

    @Value("${n8n.webhook.connect-timeout-ms:5000}")
    private long connectTimeoutMs;

    @Value("${n8n.webhook.read-timeout-ms:15000}")
    private long readTimeoutMs;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Configuramos connectTimeout (5s) y readTimeout (15s) para evitar que Tomcat se bloquee si n8n o el LLM tardan demasiado
        return builder
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}

