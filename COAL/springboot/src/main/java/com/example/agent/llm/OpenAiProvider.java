package com.example.agent.llm;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class OpenAiProvider implements LlmProvider {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;

    public OpenAiProvider(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        return doRequest(systemPrompt, List.of(
            Map.of("role", "user", "content", userMessage)
        ), null);
    }

    @Override
    public String chatWithTools(String systemPrompt, String userMessage, List<Map<String, Object>> tools) {
        return doRequest(systemPrompt, List.of(
            Map.of("role", "user", "content", userMessage)
        ), tools);
    }

    @Override
    public String chatWithToolsAndHistory(String systemPrompt, List<Map<String, Object>> history, List<Map<String, Object>> tools) {
        return doRequest(systemPrompt, history, tools);
    }

    private String doRequest(String systemPrompt, List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        String url = baseUrl + "/v1/chat/completions";
        List<Map<String, Object>> allMessages = new ArrayList<>();
        allMessages.add(Map.of("role", (Object) "system", "content", (Object) systemPrompt));
        for (Map<String, Object> msg : messages) {
            if (!"system".equals(msg.get("role"))) {
                allMessages.add(msg);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", allMessages);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return extractContent(response.getBody());
        } catch (Exception e) {
            return "{\"error\": \"OpenAI API 调用失败: " + e.getMessage() + "\"}";
        }
    }

    @Override
    public String getModelName() { return model; }

    @Override
    public boolean supportsToolCalling() { return true; }

    private String extractContent(Map<String, Object> respBody) {
        if (respBody == null) return "";
        List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message != null) {
                if (message.containsKey("tool_calls") && message.get("tool_calls") != null) {
                    try {
                        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message.get("tool_calls"));
                    } catch (Exception e) {
                        return message.get("tool_calls").toString();
                    }
                }
                Object content = message.get("content");
                if (content instanceof String && !((String) content).isEmpty()) return (String) content;
            }
        }
        return "";
    }
}
