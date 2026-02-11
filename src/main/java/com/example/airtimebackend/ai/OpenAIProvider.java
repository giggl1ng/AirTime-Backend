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
    @Value("${openrouter.api-key}")
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

        // DEBUG LOGGING
        System.out.println("🔑 API_KEY is null: " + (API_KEY == null));
        System.out.println("🔑 API_KEY is empty: " + (API_KEY != null && API_KEY.isEmpty()));
        System.out.println("🔑 API_KEY length: " + (API_KEY != null ? API_KEY.length() : "NULL"));

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
        
        CRITICAL: Respond with ONLY valid JSON. No explanations, no markdown, no ```json tags, just the JSON object starting with { and ending with }.
        
        Use this EXACT structure:
        {
          "urgency": 4,
          "priority": 4,
          "advice": "Start with the Fluid Mechanics problem set and debug the CAD file while your energy is high, then handle today's errands and prep for tomorrow's interview.",
          "motivation": "You're doing an amazing job juggling everything! Let's break this down and crush each step 🌟",
          "organizedNotes": [
            "Fluid Mechanics p-set due Thursday 11:59 PM",
            "Debug CAD file for robot arm before Wednesday meeting",
            "Soccer practice at 6 PM today - bring cleats",
            "Pick up Leo from school at 3 PM today",
            "Internship interview Wednesday 10 AM - iron shirt Tuesday night",
            "Watch Leo Saturday 2-5 PM",
            "Senior Design meeting Wednesday - confirm room with Sarah",
            "Buy chicken and spinach for meal prep",
            "Email Professor Chen about lab extension",
            "Start Thermo practice exam before Monday",
            "Do laundry ASAP",
            "Update LinkedIn profile for career fair",
            "Make pasta for dinner tonight",
            "Check FE Exam registration deadlines"
          ],
          "quickWin": "Email Professor Chen about the lab extension - it's quick and removes a worry",
          "estimatedTime": 535,
          "energyLevel": "high",
          "celebration": "You're crushing it! Every completed task moves you closer to your goals! 🎉",
          "nextSteps": [
            "Iron the shirt for the Northrop interview (10 min)",
            "Send email to Professor Chen requesting lab extension (10 min)",
            "Start Fluid Mechanics p-set (first 30 min)"
          ],
          "dopamineScore": 4,
          "calendarEvents": [
            {
              "title": "Fluid Mechanics p-set",
              "date": "2026-02-13",
              "time": "14:00",
              "duration": 90,
              "priority": "urgent",
              "notes": "Due Thursday 11:59 PM - set and debug the CAD file while your energy is high, then handle today's errands",
              "color": "red"
            },
            {
              "title": "Pick up Leo from school",
              "date": "2026-02-10",
              "time": "15:00",
              "duration": 15,
              "priority": "urgent",
              "notes": "Before soccer practice",
              "color": "orange"
            },
            {
              "title": "Soccer practice",
              "date": "2026-02-10",
              "time": "18:00",
              "duration": 90,
              "priority": "high",
              "notes": "Bring cleats!",
              "color": "green"
            },
            {
              "title": "Northrop Interview",
              "date": "2026-02-12",
              "time": "10:00",
              "duration": 60,
              "priority": "urgent",
              "notes": "Iron shirt Tuesday night - 30 min",
              "color": "red"
            },
            {
              "title": "Senior Design Meeting",
              "date": "2026-02-12",
              "time": "14:00",
              "duration": 60,
              "priority": "high",
              "notes": "Library - confirm room with Sarah. Debug CAD file before meeting",
              "color": "blue"
            },
            {
              "title": "Watch Leo",
              "date": "2026-02-15",
              "time": "14:00",
              "duration": 180,
              "priority": "medium",
              "notes": "Babysit little brother",
              "color": "green"
            }
          ],
          "timeManagementTips": [
            "Use Pomodoro for the Fluid Mechanics p-set: 25 min focus, 5 min break",
            "Tackle the p-set and CAD debugging first while energy is high",
            "Prep interview clothes tonight to avoid morning stress",
            "Batch errands: meal prep shopping + laundry in one trip"
          ]
        }
        
        RULES:
        - urgency: 1-5 (1=can wait weeks, 5=do TODAY)
        - priority: 1-5 (1=nice to have, 5=absolutely critical)
        - energyLevel: must be exactly "low", "medium", or "high"
        - estimatedTime: total minutes as a NUMBER (not string)
        - dopamineScore: 1-5 (how satisfying/rewarding this will feel)
        - calendarEvents: Extract SPECIFIC dates and times if mentioned, or suggest reasonable times
        - color: "blue", "green", "orange", "purple", "red", or "yellow"
        - color: Should represent difficulty and not random
        - Break overwhelming tasks into micro-steps
        - Identify the easiest "quick win" task
        - Be super encouraging and positive
        - Suggest realistic time blocks
        - If information is not given Infer from the tasks given
        - Consider energy levels (do hard stuff when energy is high)
        
        RESPOND WITH ONLY THE JSON OBJECT. NO OTHER TEXT.
        """, text);
}

    private BrainDumpResponse parseAIResponse(String aiResponse, String originalText) {
    try {
        System.out.println("🔍 Raw AI Response: " + aiResponse);
        
        // STEP 1: Clean up response - remove markdown code blocks
        String cleanedResponse = aiResponse
                .replace("```json", "")
                .replace("```", "")
                .trim();
        
        // STEP 2: Find the JSON object - look for { and }
        int jsonStart = cleanedResponse.indexOf('{');
        int jsonEnd = cleanedResponse.lastIndexOf('}');
        
        if (jsonStart == -1 || jsonEnd == -1 || jsonStart >= jsonEnd) {
            System.err.println("❌ No valid JSON found in response");
            return getFallbackResponse(originalText);
        }
        
        // Extract just the JSON part
        cleanedResponse = cleanedResponse.substring(jsonStart, jsonEnd + 1);
        
        System.out.println("🧹 Cleaned JSON: " + cleanedResponse.substring(0, Math.min(200, cleanedResponse.length())) + "...");
        
        // STEP 3: Try to parse the JSON
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(cleanedResponse);
        } catch (Exception parseError) {
            System.err.println("❌ JSON parse failed, trying to fix common issues...");
            
            // Try to fix common JSON issues
            cleanedResponse = cleanedResponse
                .replace("\\n", " ")           // Remove newlines in strings
                .replace("\\r", " ")           // Remove carriage returns
                .replaceAll(",\\s*}", "}")     // Remove trailing commas
                .replaceAll(",\\s*]", "]");    // Remove trailing commas in arrays
            
            try {
                jsonNode = objectMapper.readTree(cleanedResponse);
                System.out.println("✅ Fixed and parsed JSON!");
            } catch (Exception e2) {
                System.err.println("❌ Still can't parse JSON after fixes");
                System.err.println("Response was: " + cleanedResponse);
                return getFallbackResponse(originalText);
            }
        }
        
        // STEP 4: Extract all fields with safe defaults
        int urgency = jsonNode.path("urgency").asInt(3);
        int priority = jsonNode.path("priority").asInt(3);
        String advice = jsonNode.path("advice").asText("Break this into smaller, manageable steps");
        String motivation = jsonNode.path("motivation").asText("You've got this! Every step forward counts 💪");
        String quickWin = jsonNode.path("quickWin").asText("Start with the easiest task to build momentum");
        int estimatedTime = jsonNode.path("estimatedTime").asInt(30);
        String energyLevel = jsonNode.path("energyLevel").asText("medium");
        String celebration = jsonNode.path("celebration").asText("Amazing work! You're making real progress! 🎉");
        int dopamineScore = jsonNode.path("dopamineScore").asInt(3);
        
        // STEP 5: Parse arrays with extra error handling
        List<String> organizedNotes = new ArrayList<>();
        JsonNode notesNode = jsonNode.path("organizedNotes");
        if (notesNode.isArray() && notesNode.size() > 0) {
            for (JsonNode item : notesNode) {
                if (item.isTextual()) {
                    organizedNotes.add(item.asText());
                }
            }
        }
        if (organizedNotes.isEmpty()) {
            organizedNotes.add(originalText);
        }
        
        List<String> nextSteps = new ArrayList<>();
        JsonNode stepsNode = jsonNode.path("nextSteps");
        if (stepsNode.isArray() && stepsNode.size() > 0) {
            for (JsonNode item : stepsNode) {
                if (item.isTextual()) {
                    nextSteps.add(item.asText());
                }
            }
        }
        if (nextSteps.isEmpty()) {
            nextSteps.add("Take the first small step");
            nextSteps.add("Keep going");
        }
        
        List<Map<String, Object>> calendarEvents = new ArrayList<>();
        JsonNode eventsNode = jsonNode.path("calendarEvents");
        if (eventsNode.isArray() && eventsNode.size() > 0) {
            for (JsonNode eventNode : eventsNode) {
                try {
                    Map<String, Object> event = new HashMap<>();
                    event.put("title", eventNode.path("title").asText("Task"));
                    event.put("date", eventNode.path("date").asText(""));
                    event.put("time", eventNode.path("time").asText(""));
                    event.put("duration", eventNode.path("duration").asInt(30));
                    event.put("priority", eventNode.path("priority").asText("medium"));
                    event.put("notes", eventNode.path("notes").asText(""));
                    event.put("color", eventNode.path("color").asText("blue"));
                    calendarEvents.add(event);
                    
                    System.out.println("📌 Event: " + event.get("title") + " on " + event.get("date"));
                } catch (Exception eventError) {
                    System.err.println("⚠️ Skipping malformed event");
                }
            }
        }
        
        List<String> timeManagementTips = new ArrayList<>();
        JsonNode tipsNode = jsonNode.path("timeManagementTips");
        if (tipsNode.isArray() && tipsNode.size() > 0) {
            for (JsonNode item : tipsNode) {
                if (item.isTextual()) {
                    timeManagementTips.add(item.asText());
                }
            }
        }
        if (timeManagementTips.isEmpty()) {
            timeManagementTips.add("Take breaks every 25 minutes");
        }
        
        System.out.println("✅ Successfully parsed AI response");
        System.out.println("📅 Found " + calendarEvents.size() + " calendar events");
        System.out.println("📝 Found " + organizedNotes.size() + " organized notes");
        
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
        System.err.println("❌ Fatal error parsing AI response: " + e.getMessage());
        e.printStackTrace();
        System.err.println("Full response was: " + aiResponse);
        return getFallbackResponse(originalText);
    }
}

// Helper method to safely parse string arrays
private List<String> parseStringArraySafe(JsonNode jsonNode, String fieldName, String fallback) {
    List<String> result = new ArrayList<>();
    JsonNode arrayNode = jsonNode.path(fieldName);
    
    if (arrayNode.isArray()) {
        for (JsonNode item : arrayNode) {
            if (item.isTextual() && !item.asText().trim().isEmpty()) {
                result.add(item.asText().trim());
            }
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

