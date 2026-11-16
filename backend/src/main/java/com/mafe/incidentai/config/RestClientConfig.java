package com.mafe.incidentai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

// RestTemplate HTTP client with maximum wait times (timeouts) towards n8n
@Configuration
public class RestClientConfig {

    @Value("${n8n.webhook.connect-timeout-ms:5000}")
    private long connectTimeoutMs;

    @Value("${n8n.webhook.read-timeout-ms:15000}")
    private long readTimeoutMs;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Explicit HTTP/1.1: the JDK HttpClient defaults to HTTP/2 and, over plain text, it tries
        // "Upgrade: h2c"; n8n treats that request as a websocket upgrade and never answers.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // readTimeout (15s) keeps Tomcat from blocking if n8n takes too long
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder.requestFactory(() -> requestFactory).build();
    }
}
