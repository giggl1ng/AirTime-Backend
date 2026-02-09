package com.example.airtimebackend.ai;

import com.example.airtimebackend.model.BrainDumpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value; // You need this!

@Component
public class OpenAIProvider implements AIProvider {

    // 🔑 PUT YOUR OPENROUTER API KEY HERE
    // This grabs the key from the environment (Safe!)
    @Value("${OPENROUTER_API_KEY}")
    private String API_KEY;
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public BrainDumpResponse analyzeBrainDump(String text) {

        String prompt = buildADHDPrompt(text);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "openrouter/aurora-alpha");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are an ADHD-friendly task organizer and calendar assistant. Analyze brain dumps, organize tasks, set priorities, suggest calendar events with specific times, and provide encouragement. Always respond in valid JSON format with no markdown."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("🚀 Sending request to OpenRouter...");

            ResponseEntity<String> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            System.out.println("✅ Received response from OpenRouter");

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String aiResponse = rootNode
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            System.out.println("📝 AI Response: " + aiResponse);
            return parseAIResponse(aiResponse, text);

        } catch (Exception e) {
            System.err.println("❌ Error calling OpenRouter API: " + e.getMessage());
            e.printStackTrace();

            return getFallbackResponse(text);
        }
    }

    private String buildADHDPrompt(String text) {
        return String.format("""
            You are helping someone with ADHD organize their tasks and schedule.
            
            Brain dump from user:
            "%s"
            
            Analyze this and respond with ONLY valid JSON (no markdown, no ```json tags, just pure JSON).
            
            Use this EXACT structure:
            {
              "urgency": 4,
              "priority": 4,
              "advice": "Start with the biology assignment since it's due soonest",
              "motivation": "You're already ahead by planning! Let's break this down 🌟",
              "organizedNotes": [
                "Complete biology assignment (due Friday)",
                "Study for math exam (2 hours needed)",
                "Call mom this evening"
              ],
              "quickWin": "Call mom - it's quick and will make you both feel good",
              "estimatedTime": 180,
              "energyLevel": "high",
              "celebration": "You're crushing it! Each task brings you closer to your goals! 🎉",
              "nextSteps": [
                "Open biology textbook to chapter 5",
                "Read first 2 pages and take notes",
                "Write a 3-sentence summary"
              ],
              "dopamineScore": 4,
              "calendarEvents": [
                {
                  "title": "Biology Assignment",
                  "date": "2024-02-14",
                  "time": "14:00",
                  "duration": 90,
                  "priority": "high",
                  "notes": "Chapter 5 summary - due Friday",
                  "color": "blue"
                },
                {
                  "title": "Study for Math Exam",
                  "date": "2024-02-15",
                  "time": "16:00",
                  "duration": 120,
                  "priority": "urgent",
                  "notes": "Focus on algebra and geometry",
                  "color": "orange"
                },
                {
                  "title": "Call Mom",
                  "date": "2024-02-14",
                  "time": "19:00",
                  "duration": 15,
                  "priority": "medium",
                  "notes": "Catch up and say hi",
                  "color": "green"
                }
              ],
              "timeManagementTips": [
                "Use Pomodoro: 25 minutes focus, 5 minute break",
                "Tackle biology first while energy is high",
                "Reward yourself after completing each task"
              ]
            }
            
            IMPORTANT RULES:
            - urgency: 1-5 (1=can wait weeks, 5=do TODAY)
            - priority: 1-5 (1=nice to have, 5=absolutely critical)
            - energyLevel: must be exactly "low", "medium", or "high"
            - estimatedTime: total minutes as a NUMBER (not string)
            - dopamineScore: 1-5 (how satisfying/rewarding this will feel)
            - calendarEvents: Extract SPECIFIC dates and times if mentioned, or suggest reasonable times
            - color: "blue", "green", "orange", "purple", "red", or "yellow"
            - Break overwhelming tasks into micro-steps
            - Identify the easiest "quick win" task
            - Be super encouraging and positive
            - Suggest realistic time blocks
            - Consider energy levels (do hard stuff when energy is high)
            """, text);
    }

    private BrainDumpResponse parseAIResponse(String aiResponse, String originalText) {
        try {
            // Clean up response - remove markdown code blocks
            String cleanedResponse = aiResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            // If response starts with any text before {, remove it
            int jsonStart = cleanedResponse.indexOf('{');
            if (jsonStart > 0) {
                cleanedResponse = cleanedResponse.substring(jsonStart);
            }

            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

            // Extract all fields with safe defaults
            int urgency = jsonNode.path("urgency").asInt(3);
            int priority = jsonNode.path("priority").asInt(3);
            String advice = jsonNode.path("advice").asText("Break this into smaller, manageable steps");
            String motivation = jsonNode.path("motivation").asText("You've got this! Every step forward counts 💪");
            String quickWin = jsonNode.path("quickWin").asText("Start with the easiest task to build momentum");
            int estimatedTime = jsonNode.path("estimatedTime").asInt(30);
            String energyLevel = jsonNode.path("energyLevel").asText("medium");
            String celebration = jsonNode.path("celebration").asText("Amazing work! You're making real progress! 🎉");
            int dopamineScore = jsonNode.path("dopamineScore").asInt(3);

            // Parse arrays
            List<String> organizedNotes = parseStringArray(jsonNode, "organizedNotes", originalText);
            List<String> nextSteps = parseStringArray(jsonNode, "nextSteps", "Take the first small step");
            List<Map<String, Object>> calendarEvents = parseCalendarEvents(jsonNode);
            List<String> timeManagementTips = parseStringArray(jsonNode, "timeManagementTips", "Take breaks every 25 minutes");

            System.out.println("✅ Successfully parsed AI response");
            System.out.println("📅 Found " + calendarEvents.size() + " calendar events");

            return new BrainDumpResponse(
                    urgency,
                    priority,
                    advice,
                    motivation,
                    organizedNotes,
                    quickWin,
                    estimatedTime,
                    energyLevel,
                    celebration,
                    nextSteps,
                    dopamineScore,
                    calendarEvents,
                    timeManagementTips
            );

        } catch (Exception e) {
            System.err.println("❌ Error parsing AI response: " + e.getMessage());
            System.err.println("Response was: " + aiResponse);
            e.printStackTrace();
            return getFallbackResponse(originalText);
        }
    }

    private List<String> parseStringArray(JsonNode jsonNode, String fieldName, String fallback) {
        List<String> result = new ArrayList<>();
        JsonNode arrayNode = jsonNode.path(fieldName);

        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                result.add(item.asText());
            }
        }

        if (result.isEmpty()) {
            result.add(fallback);
        }

        return result;
    }

    private List<Map<String, Object>> parseCalendarEvents(JsonNode jsonNode) {
        List<Map<String, Object>> events = new ArrayList<>();
        JsonNode eventsNode = jsonNode.path("calendarEvents");

        if (eventsNode.isArray()) {
            for (JsonNode eventNode : eventsNode) {
                Map<String, Object> event = new HashMap<>();
                event.put("title", eventNode.path("title").asText("Task"));
                event.put("date", eventNode.path("date").asText(""));
                event.put("time", eventNode.path("time").asText(""));
                event.put("duration", eventNode.path("duration").asInt(30));
                event.put("priority", eventNode.path("priority").asText("medium"));
                event.put("notes", eventNode.path("notes").asText(""));
                event.put("color", eventNode.path("color").asText("blue"));
                events.add(event);

                System.out.println("📌 Event: " + event.get("title") + " on " + event.get("date"));
            }
        }

        return events;
    }

    private BrainDumpResponse getFallbackResponse(String text) {
        System.out.println("⚠️ Using fallback response");

        // Create a simple calendar event from the text
        List<Map<String, Object>> fallbackEvents = new ArrayList<>();
        Map<String, Object> event = new HashMap<>();
        event.put("title", "Organize: " + (text.length() > 30 ? text.substring(0, 30) + "..." : text));
        event.put("date", java.time.LocalDate.now().toString());
        event.put("time", "14:00");
        event.put("duration", 30);
        event.put("priority", "medium");
        event.put("notes", "AI processing unavailable - review this manually");
        event.put("color", "blue");
        fallbackEvents.add(event);

        return new BrainDumpResponse(
                3,
                3,
                "Let's break this down step by step - you've got this!",
                "Great job taking action! Planning is half the battle 🌟",
                List.of(text),
                "Start with just 5 minutes to get momentum going",
                30,
                "medium",
                "Fantastic! You're making progress! 🎉",
                List.of(
                        "Read through your task carefully",
                        "Identify the very first action step",
                        "Set a 10-minute timer and start"
                ),
                3,
                fallbackEvents,
                List.of(
                        "Try the Pomodoro technique: 25 min work, 5 min break",
                        "Start with the easiest part to build confidence",
                        "Celebrate small wins along the way"
                )
        );
    }
}