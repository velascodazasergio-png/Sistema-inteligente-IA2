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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Con app.callback.token configurado, solo quien envie X-Callback-Token puede registrar respuestas o fallos.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.callback.token=test-secret",
        "spring.jpa.show-sql=false",
        "spring.datasource.url=jdbc:h2:mem:callback_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
})
// Las pruebas de la API usan una sesion de administrador; los permisos por rol se prueban en SecurityTest
@WithMockUser(username = "sarah.jenkins@enterprise.io", roles = "ADMIN")
class CallbackTokenTest {

    private static final String BODY = "{\"aiQueryId\":1,\"suggestedDiagnosis\":\"x\",\"sources\":[]}";

    @Autowired
    private MockMvc mvc;

    @Test
    void rejectsCallbackWithoutToken() throws Exception {
        mvc.perform(post("/api/v1/ai-assistance/responses").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/ai-assistance/queries/1/failure").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsCallbackWithWrongToken() throws Exception {
        mvc.perform(post("/api/v1/ai-assistance/responses").header("X-Callback-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsCallbackWithToken() throws Exception {
        // La consulta 1 ya tiene respuesta en data.sql: pasar el filtro y llegar a la regla de negocio (409)
        mvc.perform(post("/api/v1/ai-assistance/responses").header("X-Callback-Token", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void doesNotProtectOtherEndpoints() throws Exception {
        mvc.perform(get("/api/v1/ai-assistance/history")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/ai-assistance/responses/1/ratings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
