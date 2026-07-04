package com.example.agent.agent;

import java.util.*;

/**
 * 短期记忆：单次会话的消息历史和 Token 预算管理。
 * 超过 Token 阈值时自动对旧消息做摘要压缩。
 */
public class ConversationSession {

    private final String sessionId;
    private final int maxTokens;
    private final int summaryThreshold;
    private final List<Message> history = new ArrayList<>();
    private int currentTokens = 0;
    private String summary = "";  // 被压缩的旧消息摘要

    public ConversationSession(String sessionId, int maxTokens, int summaryThreshold) {
        this.sessionId = sessionId;
        this.maxTokens = maxTokens;
        this.summaryThreshold = summaryThreshold;
    }

    public void addMessage(String role, String content) {
        int tokens = estimateTokens(content);
        if (currentTokens + tokens > maxTokens && history.size() > 4) {
            compressOldestMessages();
        }
        history.add(new Message(role, content, null, null, tokens));
        currentTokens += tokens;
    }

    /**
     * 添加 assistant 的 tool_calls 消息（DeepSeek 要求 tool 回复前必须有关联）
     */
    public void addAssistantToolCalls(List<java.util.Map<String, Object>> toolCalls) {
        // 构建精确的 OpenAI tool_calls 格式：[{"id":"xxx","type":"function","function":{"name":"xxx","arguments":"{}"}}]
        List<Map<String, Object>> formatted = new java.util.ArrayList<>();
        for (Map<String, Object> tc : toolCalls) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", tc.getOrDefault("id", "call_" + System.currentTimeMillis()));
            item.put("type", "function");
            Map<String, Object> func = new java.util.HashMap<>();
            func.put("name", tc.get("name"));
            func.put("arguments", tc.getOrDefault("arguments", "{}"));
            item.put("function", func);
            formatted.add(item);
        }
        history.add(new Message("assistant", null, null, null, 0) {
            @Override
            Map<String, Object> toMap() {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("role", "assistant");
                m.put("content", (Object) null);
                m.put("tool_calls", (Object) formatted);
                return m;
            }
        });
    }

    /**
     * 添加 tool 角色消息（需要 tool_call_id）
     */
    public void addToolMessage(Map<String, Object> toolMsg) {
        String content = (String) toolMsg.getOrDefault("content", "");
        String callId = (String) toolMsg.getOrDefault("tool_call_id", "");
        int tokens = estimateTokens(content);
        history.add(new Message("tool", content, callId, null, tokens));
        currentTokens += tokens;
    }

    public List<Map<String, Object>> getRecentHistory(int maxMessages) {
        int from = Math.max(0, history.size() - maxMessages);
        List<Map<String, Object>> result = new ArrayList<>();
        if (!summary.isEmpty()) {
            result.add(Map.of("role", (Object) "system", "content", (Object) ("[历史对话摘要] " + summary)));
        }
        for (int i = from; i < history.size(); i++) {
            Message m = history.get(i);
            result.add(m.toMap());
        }
        return result;
    }

    public List<Map<String, Object>> getFullHistory() {
        return getRecentHistory(history.size());
    }

    private void compressOldestMessages() {
        // 保留最新 2 轮，之前的做摘要
        if (history.size() <= 4) return;
        StringBuilder sb = new StringBuilder();
        int keep = 4; // 保留最新 4 条（2 轮对话）
        for (int i = 0; i < history.size() - keep; i++) {
            Message m = history.get(i);
            sb.append(m.role).append(": ").append(m.content).append("; ");
        }
        summary = sb.toString().length() > 300 ? sb.substring(0, 300) + "..." : sb.toString();

        // 移除旧消息
        int removed = history.size() - keep;
        for (int i = 0; i < removed; i++) {
            currentTokens -= history.get(0).tokens;
            history.remove(0);
        }
    }

    public void clear() {
        history.clear();
        summary = "";
        currentTokens = 0;
    }

    // ---- 估算 token 数 ----
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseChars = 0, otherChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        return (int) (chineseChars / 1.5 + otherChars / 4.0);
    }

    // ---- 内部类 ----
    private static class Message {
        final String role;
        final String content;
        final String toolCallId;
        final String name;  // assistant tool_call name
        final int tokens;

        Message(String role, String content, String toolCallId, String name, int tokens) {
            this.role = role;
            this.content = content;
            this.toolCallId = toolCallId;
            this.name = name;
            this.tokens = tokens;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("role", role);
            if (content != null) m.put("content", content);
            if (toolCallId != null) m.put("tool_call_id", toolCallId);
            if (name != null) m.put("name", name);
            return m;
        }
    }

    // ---- Getters ----
    public String getSessionId() { return sessionId; }
    public int getCurrentTokens() { return currentTokens; }
    public int getMessageCount() { return history.size(); }
}
