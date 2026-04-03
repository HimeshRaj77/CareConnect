package com.careconnectlite.service;

import com.careconnectlite.model.SupportRequest;
import com.careconnectlite.repository.SupportRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SupportRequestService {

    private static final String PROMPT_TEMPLATE =
        "Analyze the following patient request. Return ONLY a raw JSON object strictly in this format: {\"urgency\": \"High/Medium/Low\", \"summary\": \"A single sentence summary\"}. Do not include markdown tags.";

    private static final String FALLBACK_URGENCY = "UNASSIGNED";
    private static final String FALLBACK_SUMMARY = "AI triage unavailable; request saved for manual review.";

    private final SupportRequestRepository supportRequestRepository;
    private final ObjectMapper objectMapper;
    private final String llmApiKey;
    private final String llmModel;
    private final HttpClient httpClient;

    public SupportRequestService(
        SupportRequestRepository supportRequestRepository,
        ObjectMapper objectMapper,
        @Value("${llm.api.key}") String llmApiKey,
        @Value("${llm.model:gemini-2.0-flash}") String llmModel
    ) {
        this.supportRequestRepository = supportRequestRepository;
        this.objectMapper = objectMapper;
        this.llmApiKey = llmApiKey;
        this.llmModel = llmModel;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public SupportRequest processAndSaveRequest(SupportRequest request) {
        request.setId(null);

        try {
            String originalMessage = request.getOriginalMessage();
            String prompt = PROMPT_TEMPLATE + "\n\nPatient Request: " + originalMessage;
            String responseText = callGemini(prompt);
            JsonNode triageNode = parseTriageFromModel(responseText);

            request.setAiUrgency(readTextOrDefault(triageNode, "urgency", FALLBACK_URGENCY));
            request.setAiSummary(readTextOrDefault(triageNode, "summary", FALLBACK_SUMMARY));
        } catch (Exception ex) {
            request.setAiUrgency(FALLBACK_URGENCY);
            request.setAiSummary(FALLBACK_SUMMARY);
        }

        return supportRequestRepository.save(request);
    }

    public String callGemini(String prompt) throws IOException, InterruptedException {
        String encodedKey = URLEncoder.encode(llmApiKey, StandardCharsets.UTF_8);
        String modelName = llmModel.startsWith("models/") ? llmModel.substring("models/".length()) : llmModel;
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + encodedKey;

        String payload = objectMapper.createObjectNode()
            .putArray("contents")
            .addObject()
            .putArray("parts")
            .addObject()
            .put("text", prompt)
            .toString();

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("LLM request failed with status " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new IOException("LLM response did not contain a text payload");
        }

        return textNode.asText();
    }

    private JsonNode parseTriageFromModel(String modelText) throws IOException {
        String trimmed = modelText.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');

        if (start < 0 || end <= start) {
            throw new IOException("Could not locate JSON object in LLM output");
        }

        String json = trimmed.substring(start, end + 1);
        return objectMapper.readTree(json);
    }

    private String readTextOrDefault(JsonNode node, String fieldName, String fallback) {
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.asText().isBlank()) {
            return fallback;
        }
        return valueNode.asText();
    }
}
