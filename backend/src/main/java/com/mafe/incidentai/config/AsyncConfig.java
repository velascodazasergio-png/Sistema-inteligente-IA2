package com.mafe.incidentai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Enables @Async for n8n calls that must not block the HTTP response (article reindexing)
@Configuration
@EnableAsync
public class AsyncConfig {
}
