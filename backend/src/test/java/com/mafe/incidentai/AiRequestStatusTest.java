package com.mafe.incidentai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Asking the AI only moves an open incident to IN_AI_ANALYSIS; a resolved one is not reopened
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "n8n.webhook.assistance-url=http://127.0.0.1:1/webhook/analyze-incident",
        "spring.jpa.show-sql=false",
        "spring.datasource.url=jdbc:h2:mem:ai_status_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
})
// API tests use an administrator session; role permissions are tested in SecurityTest
@WithMockUser(username = "sarah.jenkins@enterprise.io", roles = "ADMIN")
class AiRequestStatusTest {

    @Autowired
    private MockMvc mvc;

    // JSON body of an AI request for the given incident
    private String request(long incidentId) {
        return """
                {"incidentId": %d, "incidentTitle": "Incident", "detailedDescription": "%s",
                 "categoryName": "Networking", "priority": "HIGH"}
                """.formatted(incidentId, "x".repeat(30));
    }

    @Test
    void resolvedIncidentKeepsItsStatus() throws Exception {
        // Incident 1 is RESOLVED in data.sql
        mvc.perform(post("/api/v1/ai-assistance/request").contentType(MediaType.APPLICATION_JSON).content(request(1)))
                .andExpect(status().isAccepted());
        mvc.perform(get("/api/v1/incidents/1")).andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void openIncidentMovesToAiAnalysis() throws Exception {
        // Incident 150 is OPEN in data.sql
        mvc.perform(post("/api/v1/ai-assistance/request").contentType(MediaType.APPLICATION_JSON).content(request(150)))
                .andExpect(status().isAccepted());
        mvc.perform(get("/api/v1/incidents/150")).andExpect(jsonPath("$.status").value("IN_AI_ANALYSIS"));
    }
}
