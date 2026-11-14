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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Peticiones invalidas que antes respondian 500: cada una debe devolver su codigo HTTP correcto
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.show-sql=false",
        "spring.datasource.url=jdbc:h2:mem:http_errors_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
})
// Las pruebas de la API usan una sesion de administrador; los permisos por rol se prueban en SecurityTest
@WithMockUser(username = "sarah.jenkins@enterprise.io", roles = "ADMIN")
class HttpErrorHandlingTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void unknownRouteIs404() throws Exception {
        mvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void unsupportedMethodIs405() throws Exception {
        mvc.perform(delete("/api/v1/incidents/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void nonJsonBodyIs415() throws Exception {
        mvc.perform(post("/api/v1/incidents").contentType(MediaType.TEXT_PLAIN).content("title=x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void incidentDescriptionLongerThan4000IsRejected() throws Exception {
        String body = """
                {"title": "Valid title", "description": "%s", "priority": "LOW", "userId": 1, "categoryId": 1}
                """.formatted("x".repeat(4001));
        mvc.perform(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.description", containsString("4000")));
    }

    @Test
    void aiContextLongerThan500IsRejected() throws Exception {
        String body = """
                {"incidentId": 1, "incidentTitle": "t", "detailedDescription": "%s",
                 "categoryName": "c", "priority": "LOW", "additionalPromptContext": "%s"}
                """.formatted("x".repeat(30), "y".repeat(501));
        mvc.perform(post("/api/v1/ai-assistance/request").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.additionalPromptContext", containsString("500")));
    }
}
