package com.open.spring.mvc.grading;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping("/api/gemini")
public class GeminiGradingController {

    @Autowired
    private GeminiGradingRepository repository;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeRequest {
        private String question;
        private String response;
    }

    // Helper to escape JSON string payloads simply
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @PostMapping("/grade")
    public ResponseEntity<Map<String, Object>> grade(@RequestBody GradeRequest req) {
        String apiUrl = System.getenv("GEMINI_API_URL");
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiUrl == null || apiKey == null) {
            return new ResponseEntity<>(
                Map.of("status", "error", "message", "GEMINI_API_URL or GEMINI_API_KEY not set"),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        try {
            // Build a concise prompt for grading
            String prompt = "Question: " + req.getQuestion() + "\\nStudent response: " + req.getResponse()
                    + "\\nPlease provide a short grade (e.g., A/B/C) and brief feedback.";

            // Build request JSON. Many Gemini/Generative APIs accept different shapes;
            // we send a simple {\"input\":\"...\"} payload and include the API key as query param.
            String jsonBody = "{\"input\":\"" + escapeJson(prompt) + "\", \"maxOutputTokens\":256}";

            String fullUrl = apiUrl + (apiUrl.contains("?") ? "&" : "?") + "key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String geminiRawReply = response.body();

            // Save record
            GeminiGrading record = new GeminiGrading(
                    req.getQuestion(),
                    req.getResponse(),
                    geminiRawReply,
                    Instant.now()
            );
            GeminiGrading saved = repository.save(record);

            return new ResponseEntity<>(
                Map.of(
                    "status", "success",
                    "id", saved.getId(),
                    "geminiReply", geminiRawReply
                ),
                HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                Map.of("status", "error", "message", e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
