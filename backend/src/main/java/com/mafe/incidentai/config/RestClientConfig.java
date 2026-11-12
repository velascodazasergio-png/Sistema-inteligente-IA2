package com.mafe.incidentai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
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
        // HTTP/1.1 explicito: el HttpClient del JDK usa HTTP/2 por defecto y en texto plano intenta
        // "Upgrade: h2c"; n8n trata esa peticion como un upgrade de websocket y nunca responde.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // readTimeout (15s) evita que Tomcat se bloquee si n8n tarda demasiado
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder.requestFactory(() -> requestFactory).build();
    }
}
