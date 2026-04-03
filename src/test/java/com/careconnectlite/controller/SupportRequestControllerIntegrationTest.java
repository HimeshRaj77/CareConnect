package com.careconnectlite.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careconnectlite.model.SupportRequest;
import com.careconnectlite.repository.SupportRequestRepository;
import com.careconnectlite.service.SupportRequestService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:careconnect_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "llm.api.key=test-api-key"
})
class SupportRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @SpyBean
    private SupportRequestService supportRequestService;

    @Test
    void shouldSaveWithFallbackWhenLlmCallFails() throws Exception {
        supportRequestRepository.deleteAll();
        doThrow(new IOException("simulated llm timeout"))
            .when(supportRequestService)
            .callGemini(anyString());

        String payload = """
            {
              "name": "Ravi Kumar",
              "contact": "+91-9999999999",
              "originalMessage": "My mother has had a high fever and breathing discomfort since last night."
            }
            """;

        mockMvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.aiUrgency", is("UNASSIGNED")))
            .andExpect(jsonPath("$.aiSummary", is("AI triage unavailable; request saved for manual review.")))
            .andExpect(jsonPath("$.status", is("PENDING")));

        SupportRequest saved = supportRequestRepository.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertEquals("UNASSIGNED", saved.getAiUrgency());
        org.junit.jupiter.api.Assertions.assertEquals("AI triage unavailable; request saved for manual review.", saved.getAiSummary());
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", saved.getStatus());
    }
}
