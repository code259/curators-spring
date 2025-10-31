package com.open.spring.mvc.geminiFRQgrading;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping("/api")
public class GeminiController {

    @Autowired
    private GeminiRepository geminiRepository;

    private final Dotenv dotenv = Dotenv.load();
    private final String geminiApiKey = dotenv.get("GEMINI_API_KEY");
    private final String geminiApiUrl = dotenv.get("GEMINI_API_URL");

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeRequest {
        private String question;
        private String answer;
    }

    // POST - Grade a student's answer
    @PostMapping("/grade")
    public ResponseEntity<?> grade(@RequestBody GradeRequest request) {
        try {
            String question = request.getQuestion();
            String answer = request.getAnswer();

            if (question == null || answer == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing question or answer"));
            }

            // Build the grading prompt
            String prompt = String.format("""
                You are an expert tutor grading a student's answer to a free-response question.
                Your task is to:
                1. Determine a grade for the student's response on a 0.55-1.0 scale, where 0.55 means they failed the question and 0.9 means it's really good. Very rarely give out a score above 0.9. Scores may be between any of these scores:
                   - 1.0: The answer addresses all parts of the question and is detailed and comprehensive.
                   - 0.9: The answer is correct and addresses most parts of the question.
                   - 0.8: The answer is correct but may be incomplete or lack detail.
                   - 0.7: The answer has significant inaccuracies or is incomplete.
                   - 0.55: The answer is incorrect or does not address the question.
                   Write the grade like this: "Grade: (0.55-1.0)/1.0"
                2. Provide detailed, constructive feedback explaining the grade.
                3. Offer very short suggestions on what the user could improve on.
                The question is: %s
                The student's response is: %s
                Format your final output with a clear heading for the grade and feedback. Also, do not use HTML or markdown formatting in your response, just simple text.
            """, question, answer);

            // Proper JSON payload with escaped characters
            String jsonPayload = String.format("""
                {
                    "contents": [{
                        "parts": [{
                            "text": "%s"    
                        }]
                    }]
                }
            """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

            // Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> httpRequest = new HttpEntity<>(jsonPayload, headers);
            String fullUrl = geminiApiUrl + "?key=" + geminiApiKey;

            // Send POST request to Gemini API
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.POST, httpRequest, String.class);

            String gradingResult = response.getBody();

            // Parse and modify response to update only the grading text
            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseNode = mapper.readTree(gradingResult);

            String extractedText = extractGradingText(gradingResult);
            ((com.fasterxml.jackson.databind.node.ObjectNode) responseNode
                    .get("candidates")
                    .get(0)
                    .get("content")
                    .get("parts")
                    .get(0))
                .put("text", extractedText);

            // Persist grading to DB
            Gemini record = new Gemini(question, answer);
            record.setGradingResult(extractedText);
            geminiRepository.save(record);

            // Add top-level feedback for frontend fallback
            ((com.fasterxml.jackson.databind.node.ObjectNode) responseNode)
                .put("feedback", extractedText);

            // Return the tree the frontend expects
            return ResponseEntity.ok(responseNode);

        } catch (HttpClientErrorException.TooManyRequests e) {
            return ResponseEntity.status(429).body(Map.of("error", "Gemini quota exceeded. Please try again later."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    // GET - Fetch all grading results (no user filtering)
    @GetMapping("/grades")
    public ResponseEntity<?> getGrades() {
        List<Gemini> results = geminiRepository.findAll();
        return ResponseEntity.ok(Map.of(
            "count", results.size(),
            "results", results
        ));
    }

    // Helper method to extract grading text from Gemini API response
    private String extractGradingText(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            
            // Navigate: candidates[0].content.parts[0].text
            JsonNode textNode = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");
            
            if (textNode.isTextual()) {
                return textNode.asText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return "Error parsing grading result";
    }
}
