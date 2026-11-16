package com.mafe.incidentai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RF03: when an article changes, the backend notifies n8n workflow 01 (after the commit) to reindex it.
// No @Transactional: the notification is only sent when the transaction really commits.
// A local HttpServer plays the n8n webhook and records every request it receives.
@SpringBootTest
@AutoConfigureMockMvc
// API tests use an administrator session; role permissions are tested in SecurityTest
@WithMockUser(username = "sarah.jenkins@enterprise.io", roles = "ADMIN")
class KnowledgeIndexingTest {

    private static final BlockingQueue<String> RECEIVED = new LinkedBlockingQueue<>();
    private static final HttpServer N8N = startFakeN8n();

    // Local HTTP server that plays the n8n indexing webhook and records each call
    private static HttpServer startFakeN8n() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/webhook/index-knowledge-base", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                RECEIVED.add(exchange.getRequestHeaders().getFirst("X-N8N-Token") + " " + body);
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Points the backend at the fake n8n webhook
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("n8n.webhook.index-url",
                () -> "http://127.0.0.1:" + N8N.getAddress().getPort() + "/webhook/index-knowledge-base");
        registry.add("n8n.webhook.auth-header-value", () -> "index-secret");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:indexing_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
    }

    // Stops the fake n8n server after the tests
    @AfterAll
    static void stopFakeN8n() {
        N8N.stop(0);
    }

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void clearRequests() {
        RECEIVED.clear();
    }

    @Test
    void statusChangeTriggersReindexWithAuthHeader() throws Exception {
        mvc.perform(patch("/api/v1/knowledge-articles/3/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());

        assertThat(RECEIVED.poll(5, TimeUnit.SECONDS)).isEqualTo("index-secret {\"articleId\":3}");

        // Restore the article so other tests are not affected
        mvc.perform(patch("/api/v1/knowledge-articles/3/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
        assertThat(RECEIVED.poll(5, TimeUnit.SECONDS)).isEqualTo("index-secret {\"articleId\":3}");
    }

    @Test
    void creatingAnUnverifiedArticleDoesNotTriggerIndexing() throws Exception {
        mvc.perform(post("/api/v1/knowledge-articles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Spooler cleanup runbook",
                         "problem": "Print spooler hangs every morning on finance printers.",
                         "symptoms": "Jobs stay queued", "cause": "Corrupt spool files",
                         "solution": "Stop the spooler, clear the spool folder and start it again.",
                         "categoryId": 1, "authorUserId": 1}"""))
                .andExpect(status().isCreated());

        assertThat(RECEIVED.poll(1500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void failedChangeDoesNotTriggerIndexing() throws Exception {
        mvc.perform(patch("/api/v1/knowledge-articles/9999/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isNotFound());

        assertThat(RECEIVED.poll(1500, TimeUnit.MILLISECONDS)).isNull();
    }
}
