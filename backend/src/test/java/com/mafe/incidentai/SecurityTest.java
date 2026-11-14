package com.mafe.incidentai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Inicio de sesion con JWT y permisos de los tres roles, con tokens reales emitidos por /auth/login.
// Usuarios de data.sql: 1 Sarah (ADMIN), 2 Marcus (SUPPORT_TECHNICIAN), 5 Laura (EMPLOYEE).
// Laura reporto el incidente 150 (OPEN); el incidente 2 es de otra persona.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.security.demo-password=Demo-Password-1",
        "app.jwt.secret=test-secret-with-at-least-32-characters!!",
        "app.callback.token=service-secret",
        "n8n.webhook.assistance-url=http://127.0.0.1:1/webhook/analyze-incident",
        "spring.jpa.show-sql=false",
        "spring.datasource.url=jdbc:h2:mem:security_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
})
class SecurityTest {

    private static final String ADMIN = "sarah.jenkins@enterprise.io";
    private static final String TECH = "marcus.vance@enterprise.io";
    private static final String EMPLOYEE = "laura.chen@enterprise.io";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo-Password-1\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }

    private MockHttpServletRequestBuilder as(String email, MockHttpServletRequestBuilder request) throws Exception {
        return request.header("Authorization", "Bearer " + login(email));
    }

    // ---------- Inicio de sesion ----------

    @Test
    void loginReturnsTokenAndUserRole() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Marcus.Vance@Enterprise.io\",\"password\":\"Demo-Password-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.id").value(2))
                .andExpect(jsonPath("$.user.role").value("SUPPORT_TECHNICIAN"));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSame401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@enterprise.io\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginValidatesFields() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    void meReturnsTheSessionUser() throws Exception {
        mvc.perform(as(EMPLOYEE, get("/api/v1/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMPLOYEE))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    // ---------- Sin sesion o token invalido ----------

    @Test
    void requestsWithoutTokenAre401() throws Exception {
        mvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void tamperedTokenIs401() throws Exception {
        String token = login(ADMIN);
        mvc.perform(get("/api/v1/incidents").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer error=\"invalid_token\""))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    void documentationStaysPublic() throws Exception {
        mvc.perform(get("/api-docs")).andExpect(status().isOk());
    }

    // ---------- Administrador ----------

    @Test
    void adminCanVerifyArticles() throws Exception {
        mvc.perform(as(ADMIN, patch("/api/v1/knowledge-articles/1/status"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"verified\":true}"))
                .andExpect(status().isOk());
    }

    // ---------- Tecnico ----------

    @Test
    void technicianManagesIncidentsAndSeesAnalytics() throws Exception {
        mvc.perform(as(TECH, patch("/api/v1/incidents/150/status"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED_TO_TECH\"}"))
                .andExpect(status().isOk());
        mvc.perform(as(TECH, get("/api/v1/analytics/dashboard"))).andExpect(status().isOk());
        mvc.perform(as(TECH, get("/api/v1/users"))).andExpect(status().isOk());
    }

    @Test
    void technicianCannotVerifyArticles() throws Exception {
        mvc.perform(as(TECH, patch("/api/v1/knowledge-articles/1/status"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"verified\":false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }

    // ---------- Empleado ----------

    @Test
    void employeeOnlyListsOwnIncidents() throws Exception {
        mvc.perform(as(EMPLOYEE, get("/api/v1/incidents").param("userId", "7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasSize(0))))
                .andExpect(jsonPath("$[*].userId", everyItem(is(5))));
    }

    @Test
    void employeeCannotOpenSomeoneElsesIncident() throws Exception {
        mvc.perform(as(EMPLOYEE, get("/api/v1/incidents/2")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only access your own incidents"));
        mvc.perform(as(EMPLOYEE, get("/api/v1/ai-assistance/incidents/2/history"))).andExpect(status().isForbidden());
        mvc.perform(as(EMPLOYEE, get("/api/v1/incidents/150"))).andExpect(status().isOk());
    }

    @Test
    void employeeReportsUnderTheirOwnName() throws Exception {
        String body = """
                {"title": "Printer offline", "description": "The third floor printer does not answer.",
                 "priority": "LOW", "userId": 1, "categoryId": 1}
                """;
        mvc.perform(as(EMPLOYEE, post("/api/v1/incidents")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(5));
    }

    @Test
    void employeeCannotChangeStatusOrSeeAnalyticsOrUsers() throws Exception {
        mvc.perform(as(EMPLOYEE, patch("/api/v1/incidents/150/status"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(as(EMPLOYEE, get("/api/v1/analytics/dashboard"))).andExpect(status().isForbidden());
        mvc.perform(as(EMPLOYEE, get("/api/v1/users"))).andExpect(status().isForbidden());
        mvc.perform(as(EMPLOYEE, post("/api/v1/knowledge-articles")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeOnlySeesPublishedArticles() throws Exception {
        mvc.perform(as(EMPLOYEE, get("/api/v1/knowledge-articles").param("verified", "false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].verified", everyItem(is(true))))
                .andExpect(jsonPath("$[*].active", everyItem(is(true))));
    }

    @Test
    void employeeCanAskTheAssistantOnlyAboutOwnIncidents() throws Exception {
        String request = """
                {"incidentId": %d, "incidentTitle": "Incident", "detailedDescription": "%s",
                 "categoryName": "Networking", "priority": "HIGH"}
                """;
        mvc.perform(as(EMPLOYEE, post("/api/v1/ai-assistance/request")).contentType(MediaType.APPLICATION_JSON)
                        .content(request.formatted(2, "x".repeat(30))))
                .andExpect(status().isForbidden());
        mvc.perform(as(EMPLOYEE, post("/api/v1/ai-assistance/request")).contentType(MediaType.APPLICATION_JSON)
                        .content(request.formatted(150, "x".repeat(30))))
                .andExpect(status().isAccepted());
    }

    @Test
    void employeeHistoryOnlyContainsOwnIncidents() throws Exception {
        mvc.perform(as(EMPLOYEE, get("/api/v1/ai-assistance/history")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].incidentId", not(org.hamcrest.Matchers.hasItem(2))));
    }

    // ---------- n8n (token de servicio) ----------

    @Test
    void n8nReadsArticlesWithServiceTokenOnly() throws Exception {
        mvc.perform(get("/api/v1/knowledge-articles").header("X-Callback-Token", "service-secret"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/knowledge-articles/1").header("X-Callback-Token", "service-secret"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/knowledge-articles").header("X-Callback-Token", "wrong"))
                .andExpect(status().isUnauthorized());
        // El token de servicio no abre el resto de la API
        mvc.perform(get("/api/v1/incidents").header("X-Callback-Token", "service-secret"))
                .andExpect(status().isForbidden());
    }
}
