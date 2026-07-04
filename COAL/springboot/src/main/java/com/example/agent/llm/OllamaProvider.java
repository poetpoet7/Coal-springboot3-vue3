package com.example.agent.llm;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class OllamaProvider implements LlmProvider {

    private final String baseUrl;
    private final String model;
    private final RestTemplate restTemplate;

    public OllamaProvider(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        return doChatRequest(systemPrompt, List.of(
            Map.of("role", "user", "content", userMessage)
        ), null);
    }

    @Override
    public String chatWithTools(String systemPrompt, String userMessage, List<Map<String, Object>> tools) {
        return doChatRequest(systemPrompt, List.of(
            Map.of("role", "user", "content", userMessage)
        ), tools);
    }

    @Override
    public String chatWithToolsAndHistory(String systemPrompt, List<Map<String, Object>> history, List<Map<String, Object>> tools) {
        return doChatRequest(systemPrompt, history, tools);
    }

    private String doChatRequest(String systemPrompt, List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        String url = baseUrl + "/api/chat";
        List<Map<String, Object>> allMessages = new ArrayList<>();
        allMessages.add(Map.of("role", (Object) "system", "content", (Object) systemPrompt));
        // 过滤掉历史中的 system 消息，避免重复
        for (Map<String, Object> msg : messages) {
            if (!"system".equals(msg.get("role"))) {
                allMessages.add(msg);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", allMessages);
        body.put("stream", false);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return extractContent(response.getBody());
        } catch (Exception e) {
            return "{\"error\": \"Ollama API 调用失败: " + e.getMessage() + "\"}";
        }
    }

    @Override
    public String getModelName() { return model; }

    @Override
    public boolean supportsToolCalling() { return true; }

    private String extractContent(Map<String, Object> respBody) {
        if (respBody == null) return "";
        if (respBody.containsKey("message")) {
            Map<String, Object> message = (Map<String, Object>) respBody.get("message");
            Object content = message.get("content");
            if (content instanceof String) return (String) content;
            if (message.containsKey("tool_calls")) {
                return message.get("tool_calls").toString();
            }
        }
        return respBody.toString();
    }
}
