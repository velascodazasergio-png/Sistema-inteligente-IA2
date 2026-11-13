package com.mafe.incidentai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Habilita @Async para las llamadas a n8n que no deben bloquear la respuesta HTTP (reindexacion de articulos)
@Configuration
@EnableAsync
public class AsyncConfig {
}
