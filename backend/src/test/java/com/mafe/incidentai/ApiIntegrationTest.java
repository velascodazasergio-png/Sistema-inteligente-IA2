package com.mafe.incidentai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de integracion de la API REST sobre H2 con los datos de data.sql (150 incidentes).
// Cada prueba se revierte al terminar (@Transactional), asi no se afectan entre si.
// n8n apunta a un puerto cerrado para simular que el servicio de IA no esta disponible.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "n8n.webhook.assistance-url=http://127.0.0.1:1/webhook/analyze-incident",
        "n8n.webhook.connect-timeout-ms=500",
        "spring.jpa.show-sql=false",
        "spring.datasource.url=jdbc:h2:mem:api_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
})
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    @Nested
    @DisplayName("Incidentes (RF01)")
    class Incidents {

        @Test
        void listsSeedIncidents() throws Exception {
            mvc.perform(get("/api/v1/incidents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", is(150)));
        }

        @Test
        void filtersByStatus() throws Exception {
            mvc.perform(get("/api/v1/incidents").param("status", "OPEN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].status", everyItem(is("OPEN"))));
        }

        @Test
        void rejectsUnknownEnumValueWith400() throws Exception {
            mvc.perform(get("/api/v1/incidents").param("priority", "URGENT"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void validatesNewIncident() throws Exception {
            postJson("/api/v1/incidents", "{\"title\":\"abc\",\"description\":\"too short\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.title").exists())
                    .andExpect(jsonPath("$.details.description").exists())
                    .andExpect(jsonPath("$.details.categoryId").exists());
        }

        @Test
        void createsIncidentAsOpen() throws Exception {
            postJson("/api/v1/incidents", """
                    {"title":"Printer queue stuck","description":"Jobs stay queued on the floor 2 printer since this morning",
                     "priority":"MEDIUM","userId":5,"categoryId":6}""")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("OPEN")))
                    .andExpect(jsonPath("$.categoryName", is("Endpoint & Hardware")));
        }

        @Test
        void closedIncidentCannotChangeStatus() throws Exception {
            mvc.perform(patch("/api/v1/incidents/150/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                    .andExpect(status().isOk());
            mvc.perform(patch("/api/v1/incidents/150/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        void returnsNotFoundForMissingIncident() throws Exception {
            mvc.perform(get("/api/v1/incidents/9999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Analitica (RF06)")
    class Analytics {

        @Test
        void dashboardMatchesSeedData() throws Exception {
            mvc.perform(get("/api/v1/analytics/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalIncidents", is(150)))
                    .andExpect(jsonPath("$.resolvedIncidents", is(130)))
                    .andExpect(jsonPath("$.resolutionRatePercentage", is(86.67)))
                    .andExpect(jsonPath("$.monthlyTrends.length()", is(3)));
        }

        @Test
        void dashboardAppliesFilters() throws Exception {
            mvc.perform(get("/api/v1/analytics/dashboard").param("categoryId", "1").param("priority", "HIGH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.topCategories.length()", is(1)))
                    .andExpect(jsonPath("$.topCategories[0].categoryId", is(1)));
        }

        @Test
        void rejectsInvertedDateRange() throws Exception {
            mvc.perform(get("/api/v1/analytics/dashboard")
                            .param("startDate", "2026-12-01T00:00:00").param("endDate", "2026-01-01T00:00:00"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Base de conocimiento (RF02)")
    class Knowledge {

        private static final String ARTICLE = """
                {"title":"Printer queue stuck","problem":"Print jobs stay queued on the printer.",
                 "symptoms":"Queue shows Error - Printing.","cause":"","solution":"Restart the spooler and clear the queue.",
                 "categoryId":6,"authorUserId":2}""";

        @Test
        void composesContentFromSectionsAndStartsUnverified() throws Exception {
            postJson("/api/v1/knowledge-articles", ARTICLE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.active", is(true)))
                    .andExpect(jsonPath("$.verified", is(false)))
                    .andExpect(jsonPath("$.cause").doesNotExist())
                    .andExpect(jsonPath("$.content", containsString("Problema: Print jobs")))
                    .andExpect(jsonPath("$.content", containsString("Solucion: Restart")));
        }

        @Test
        void editingContentResetsVerification() throws Exception {
            long id = json(postJson("/api/v1/knowledge-articles", ARTICLE)).get("id").asLong();
            mvc.perform(patch("/api/v1/knowledge-articles/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"verified\":true}"))
                    .andExpect(jsonPath("$.verified", is(true)));
            mvc.perform(put("/api/v1/knowledge-articles/" + id).contentType(MediaType.APPLICATION_JSON)
                            .content(ARTICLE.replace("Restart the spooler", "Reinstall the driver")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified", is(false)));
        }

        @Test
        void searchesOnlyUsableArticles() throws Exception {
            mvc.perform(get("/api/v1/knowledge-articles").param("active", "true").param("verified", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].verified", everyItem(is(true))))
                    .andExpect(jsonPath("$[*].active", everyItem(is(true))));
        }
    }

    @Nested
    @DisplayName("Asistencia IA (RF04, RF05, RF08)")
    class AiAssistance {

        private static final String REQUEST = """
                {"incidentId":150,"incidentTitle":"PostgreSQL pool exhausted","categoryName":"Database & Storage",
                 "detailedDescription":"HikariPool timeouts on node 2 every ten minutes","priority":"CRITICAL"}""";

        private long requestQuery() throws Exception {
            return json(postJson("/api/v1/ai-assistance/request", REQUEST).andExpect(status().isAccepted())).get("aiQueryId").asLong();
        }

        @Test
        void marksCommunicationErrorWhenN8nIsDown() throws Exception {
            postJson("/api/v1/ai-assistance/request", REQUEST)
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.queryStatus", is("COMMUNICATION_ERROR")));
            mvc.perform(get("/api/v1/ai-assistance/incidents/150/history"))
                    .andExpect(jsonPath("$[0].failureCode", is("N8N_UNREACHABLE")));
        }

        @Test
        void rejectsShortDescription() throws Exception {
            postJson("/api/v1/ai-assistance/request", REQUEST.replace("HikariPool timeouts on node 2 every ten minutes", "slow db"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void responseWithoutSourcesIsNoResults() throws Exception {
            long queryId = requestQuery();
            postJson("/api/v1/ai-assistance/responses", """
                    {"aiQueryId":%d,"suggestedDiagnosis":"Sin informacion suficiente","confidenceScore":0,"sources":[]}""".formatted(queryId))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.queryStatus", is("NO_RESULTS")));
            mvc.perform(get("/api/v1/ai-assistance/history").param("status", "NO_RESULTS"))
                    .andExpect(jsonPath("$[*].aiQueryId", hasItem((int) queryId)));
        }

        @Test
        void responseWithSourcesIsCompletedAndCanBeRatedOnce() throws Exception {
            long queryId = requestQuery();
            long responseId = json(postJson("/api/v1/ai-assistance/responses", """
                    {"aiQueryId":%d,"suggestedDiagnosis":"Pool exhausted","solutionSteps":"1. Tune maximumPoolSize","confidenceScore":0.9,
                     "sources":[{"knowledgeArticleId":5,"sourceTitle":"HikariCP runbook","similarityScore":0.91}]}""".formatted(queryId))
                    .andExpect(jsonPath("$.queryStatus", is("COMPLETED")))).get("aiResponseId").asLong();

            String rating = "{\"evaluatorUserId\":2,\"score\":5,\"helpful\":true,\"feedbackComment\":\"Worked\"}";
            postJson("/api/v1/ai-assistance/responses/" + responseId + "/ratings", rating).andExpect(status().isCreated());
            postJson("/api/v1/ai-assistance/responses/" + responseId + "/ratings", rating).andExpect(status().isConflict());
        }

        @Test
        void failureReportMarksQueryAsFailed() throws Exception {
            long queryId = requestQuery();
            postJson("/api/v1/ai-assistance/queries/" + queryId + "/failure", """
                    {"aiQueryId":%d,"errorCode":"EMBEDDING_FALLO","message":"No se pudo generar el embedding"}""".formatted(queryId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.queryStatus", is("FAILED")));
            mvc.perform(get("/api/v1/ai-assistance/incidents/150/history"))
                    .andExpect(jsonPath("$[0].queryStatus", is("FAILED")))
                    .andExpect(jsonPath("$[0].failureMessage", is("No se pudo generar el embedding")));
        }

        @Test
        void failureCannotOverrideAResponse() throws Exception {
            long queryId = requestQuery();
            postJson("/api/v1/ai-assistance/responses", """
                    {"aiQueryId":%d,"suggestedDiagnosis":"Sin informacion suficiente","sources":[]}""".formatted(queryId));
            postJson("/api/v1/ai-assistance/queries/" + queryId + "/failure", "{\"errorCode\":\"LLM_FALLO\",\"message\":\"late failure\"}")
                    .andExpect(status().isConflict());
        }

        @Test
        void closedIncidentCannotBeAnalyzed() throws Exception {
            mvc.perform(patch("/api/v1/incidents/150/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"));
            postJson("/api/v1/ai-assistance/request", REQUEST).andExpect(status().isConflict());
        }
    }
}
