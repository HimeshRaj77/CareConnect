package com.careconnectlite.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.careconnectlite.model.SupportRequest;
import com.careconnectlite.repository.SupportRequestRepository;
import java.util.Set;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:careconnect_live_ai_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_AI_TEST", matches = "true")
class SupportRequestServiceLiveAiIntegrationTest {

    private static final Set<String> VALID_URGENCIES = Set.of("HIGH", "MEDIUM", "LOW");

    @Autowired
    private SupportRequestService supportRequestService;

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @Test
    void shouldCallGeminiAndPersistEnrichedRequest() {
        supportRequestRepository.deleteAll();

        SupportRequest request = new SupportRequest();
        request.setName("Anita Sharma");
        request.setContact("+91-9000000000");
        request.setOriginalMessage("My father has chest pain and dizziness since morning and looks very weak.");

        SupportRequest saved = supportRequestService.processAndSaveRequest(request);

        assertNotNull(saved.getId(), "Saved request should have a generated ID");
        assertNotNull(saved.getAiSummary(), "AI summary should be populated");
        assertFalse(saved.getAiSummary().isBlank(), "AI summary should not be blank");

        assertNotNull(saved.getAiUrgency(), "AI urgency should be populated");
        assertFalse(saved.getAiUrgency().isBlank(), "AI urgency should not be blank");
        assertTrue(
            !"UNASSIGNED".equalsIgnoreCase(saved.getAiUrgency()),
            "LLM call appears to have failed; urgency fallback UNASSIGNED was used"
        );
        assertTrue(
            VALID_URGENCIES.contains(saved.getAiUrgency().trim().toUpperCase()),
            "Urgency should be one of High/Medium/Low"
        );

        assertNotNull(saved.getStatus(), "Status should be set by default");
        assertTrue("PENDING".equalsIgnoreCase(saved.getStatus()), "Default status should be PENDING");
        assertTrue(supportRequestRepository.count() == 1, "Exactly one record should be persisted");
    }
}
