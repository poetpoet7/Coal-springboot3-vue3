package com.example.agent.llm;

import java.util.List;
import java.util.Map;

/**
 * LLM Provider 统一抽象接口。
 */
public interface LlmProvider {

    String chat(String systemPrompt, String userMessage);

    String chatWithTools(String systemPrompt, String userMessage, List<Map<String, Object>> tools);

    String chatWithToolsAndHistory(String systemPrompt, List<Map<String, Object>> history, List<Map<String, Object>> tools);

    String getModelName();

    boolean supportsToolCalling();
}
