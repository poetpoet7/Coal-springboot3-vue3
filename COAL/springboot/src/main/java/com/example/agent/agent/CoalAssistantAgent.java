package com.example.agent.agent;

import com.example.agent.llm.LlmProvider;
import com.example.agent.rag.KnowledgeService;
import com.example.agent.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 煤炭统计智能助手 —— 核心 Agent。
 * 统一入口，通过 LLM Function Calling 自动判断用户意图并调用相应 Tool。
 */
@Service
public class CoalAssistantAgent {

    @Resource private LlmProvider llmProvider;
    @Resource private ToolRegistry toolRegistry;
    @Resource private AccessGuard accessGuard;
    @Resource private KnowledgeService knowledgeService;
    @Resource private ConversationService conversationService;

    @Value("${coal.ai.memory.short-term.max-tokens:4000}")
    private int maxTokens;

    @Value("${coal.ai.memory.short-term.summary-threshold:3200}")
    private int summaryThreshold;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Agent 同步对话入口。
     */
    public AgentResponse chat(String userMessage, String sessionId, SecurityContext secCtx) {
        ConversationSession session = conversationService.getOrCreate(sessionId, maxTokens, summaryThreshold);

        // 保存用户消息
        conversationService.saveMessage(secCtx.getUserId(), session.getSessionId(), "user", userMessage, null, null, 0);
        session.addMessage("user", userMessage);

        // 构建 System Prompt（含安全上下文 + 人设约束）
        String systemPrompt = buildSystemPrompt(secCtx);

        // 获取工具 Schema
        List<Map<String, Object>> toolSchemas = toolRegistry.getToolSchemas();

        // === ReAct 多轮循环：LLM ↔ Tool 执行，直到不需要再调工具 ===
        List<ToolCall> allToolCalls = new ArrayList<>();
        String finalAnswer = "";
        int maxRounds = 2;  // 最多2轮：第1轮查数据，第2轮总结

        for (int round = 0; round < maxRounds; round++) {
            String llmResponse = llmProvider.chatWithToolsAndHistory(
                systemPrompt, session.getFullHistory(), toolSchemas);

            List<ToolCall> toolCalls = extractToolCalls(llmResponse);

            if (toolCalls.isEmpty()) {
                // LLM 没有调用工具 → 直接返回文本回答
                finalAnswer = llmResponse;
                break;
            }

            // 先把 assistant 的 tool_calls 消息写入历史（构建精确的 OpenAI 格式）
            List<Map<String, Object>> formattedCalls = new ArrayList<>();
            for (ToolCall tc : toolCalls) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", tc.getCallId());
                item.put("name", tc.getToolName());
                item.put("arguments", "");
                try { item.put("arguments", objectMapper.writeValueAsString(tc.getParams())); } catch (Exception ignored) {}
                formattedCalls.add(item);
            }
            session.addAssistantToolCalls(formattedCalls);

            // 执行本轮工具调用
            StringBuilder toolResults = new StringBuilder();
            for (ToolCall tc : toolCalls) {
                allToolCalls.add(tc);
                ToolDefinition tool = toolRegistry.get(tc.toolName);

                if (tool == null) {
                    tc.setAccessDecision("NOT_FOUND");
                    tc.setFallbackReason("工具 " + tc.toolName + " 未在 ToolRegistry 中注册");
                    toolResults.append("[错误] 未知工具: ").append(tc.toolName).append("\n");
                    continue;
                }

                // AccessGuard 检查
                if (!accessGuard.checkAndLog(tool, new AccessGuard.MapParams(tc.params),
                    secCtx.getUserId(), session.getSessionId())) {
                    tc.setAccessDecision("BLOCKED");
                    tc.setFallbackReason("AccessGuard 拒绝 WRITE 工具 " + tc.toolName);
                    toolResults.append("[拒绝] 工具 ").append(tc.toolName)
                        .append(" 需要写权限，Agent 不可执行。\n");
                    continue;
                }

                // 权限校验：检查目标单位是否在用户可访问范围内
                if ("query_stat_data".equals(tc.toolName) && tc.params.containsKey("unitId")) {
                    Integer targetUnitId = toInteger(tc.params.get("unitId"));
                    if (targetUnitId != null && secCtx.isForbidden(targetUnitId)) {
                        tc.setAccessDecision("PERMISSION_DENIED");
                        tc.setFallbackReason("用户(" + secCtx.getDanweiName() + ")无权访问单位ID=" + targetUnitId);
                        toolResults.append("[权限拒绝] 您（").append(secCtx.getDanweiName())
                            .append("）无权访问单位ID=").append(targetUnitId)
                            .append(" 的数据。只能查看").append(secCtx.getDanweiName())
                            .append("及下属单位的数据。\n");
                        continue;
                    }
                }

                // 执行工具
                String toolResult;
                if ("search_knowledge".equals(tc.toolName)) {
                    String query = (String) tc.params.getOrDefault("query", userMessage);
                    toolResult = knowledgeService.search(query);
                } else {
                    toolResult = tool.execute(tc.params);
                }

                // 填充 trace 字段
                tc.setAccessDecision("ALLOWED");
                String summary = toolResult != null && toolResult.length() > 200
                    ? toolResult.substring(0, 200) + "..." : toolResult;
                tc.setToolResultSummary(summary);
                if (toolResult != null && toolResult.startsWith("查询失败")) {
                    tc.setFallbackReason("Tool 执行返回错误: " + summary);
                }

                toolResults.append("工具 ").append(tc.toolName).append(" 返回结果：\n").append(toolResult).append("\n");

                // 保存到对话历史（tool 角色消息必须带 tool_call_id）
                Map<String, Object> toolMsg = new java.util.HashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", tc.callId);
                toolMsg.put("content", toolResult);
                session.addToolMessage(toolMsg);
                conversationService.saveMessage(secCtx.getUserId(), session.getSessionId(), "tool",
                    toolResult, tc.toolName, tc.params.toString(), 0);
            }

            // 最后一轮强制生成最终回答（不传 tools，LLM 就不会调用工具）
            if (round == maxRounds - 1) {
                String finalPrompt = "基于以上所有工具调用结果，用自然语言向用户汇报。要求：简洁、结构化、按月列出、标注数据来源。不要调用任何工具。\n\n" + toolResults;
                finalAnswer = llmProvider.chat(systemPrompt, finalPrompt);
            }
            // 中间轮不插入额外消息，让 LLM 自然决定是否需要继续调工具
        }

        // 保存最终回复
        session.addMessage("assistant", finalAnswer);
        conversationService.saveMessage(secCtx.getUserId(), session.getSessionId(), "assistant", finalAnswer,
            null, null, session.getCurrentTokens());

        return new AgentResponse(session.getSessionId(), finalAnswer, allToolCalls);
    }

    /**
     * 构建 System Prompt（注入单位树、当前时间等上下文）。
     */
    private String buildSystemPrompt(SecurityContext secCtx) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return """
            你是兖矿集团煤炭业务统计系统（Coal-AI）的智能助手。
            你的唯一职责是帮助用户查询煤炭统计数据、理解业务规则、检查数据质量。
            你是专业的煤炭业务助手，不是通用聊天机器人。

            ## 人设边界（必须严格遵守！）
            - 只回答与煤炭统计、填报、审批、数据查询、业务规则相关的问题。
            - 用户与你闲聊（如"你好"、"今天天气怎么样"、"讲个笑话"等），统一回复："我是煤炭业务助手，专注于数据查询和业务咨询。请问有什么统计业务需要帮助？"
            - 不要讨论政治、娱乐、体育、科技新闻等与煤炭业务无关的话题。
            - 所有回答基于真实工具查询结果，不编造、不猜测。

            ## 数据安全规则
            1. 只能查询和分析数据，不能修改任何数据。上市公司数据涉及合规红线。
            2. **工具返回"查询结果为空"就是答案！直接告诉用户，不要换参数重试。**
            3. 如果用户问超出你能力范围的事，明确告知并建议操作路径。
            4. 涉及业务规则的查询，优先使用 search_knowledge 工具检索制度文档。
            5. 回答极其简洁：先说结论（数据是多少）再说来源。

            ## 多步工具调用规则
            6. 典型流程：用户提到单位名称 → 先调 get_unit_tree 获取单位ID → 再调 query_stat_data 查数据。
            7. 【关键】query_stat_data 不传 month 参数即可一次性获取全年12个月数据！不要逐月查询。
            8. 工具返回结果后判断数据是否足够。不够就继续调工具，够了就给出最终回答。
            9. 总共调用的工具次数不要超过5次。

            ## 当前日期
            """ + currentDate + "\n\n" + secCtx.toSystemPromptRules() + """

            ## 可用模块
            - jingyingzongzhi: 生产经营总值
            - chanpinchanxiaocun: 主要工业产品产销存
            - chukouchanpin: 主要出口产品情况
            - zhuyaojishujingji: 主要技术经济指标
            - dianlijishu: 电力企业主营业务技术指标
            - huagongyewu: 化工企业主营业务技术指标
            - feimeilaodonggongzi: 非煤产业劳动工资

            ## 可用工具
            你拥有 10 个工具，根据用户意图选择合适的工具。
            """;
    }

    /**
     * 解析 LLM 返回中的工具调用。
     */
    private List<ToolCall> extractToolCalls(String llmResponse) {
        List<ToolCall> calls = new ArrayList<>();
        if (llmResponse == null || llmResponse.isEmpty()) return calls;
        try {
            // OpenAI/DeepSeek 格式: [{"id":"call_xxx","type":"function","function":{"name":"xxx","arguments":"{}"}}]
            if (llmResponse.trim().startsWith("[{")) {
                List<Map<String, Object>> rawList = objectMapper.readValue(
                    llmResponse, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> item : rawList) {
                    String callId = (String) item.getOrDefault("id", "call_" + System.currentTimeMillis());
                    Map<String, Object> func = (Map<String, Object>) item.get("function");
                    if (func != null) {
                        String name = (String) func.get("name");
                        Object argsObj = func.get("arguments");
                        Map<String, Object> args = Map.of();
                        if (argsObj instanceof String && !((String) argsObj).isEmpty()) {
                            try {
                                args = objectMapper.readValue((String) argsObj,
                                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                            } catch (Exception ignored) {}
                        } else if (argsObj instanceof Map) {
                            args = (Map<String, Object>) argsObj;
                        }
                        if (name != null) {
                            calls.add(new ToolCall(callId, name, args));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // 非 JSON 格式的纯文本回答，没有工具调用
        }
        return calls;
    }

    // ---- 响应封装 ----
    public static class AgentResponse {
        private final String sessionId;
        private final String content;
        private final List<ToolCall> toolCalls;

        public AgentResponse(String sessionId, String content, List<ToolCall> toolCalls) {
            this.sessionId = sessionId;
            this.content = content;
            this.toolCalls = toolCalls;
        }
        public String getSessionId() { return sessionId; }
        public String getContent() { return content; }
        public List<ToolCall> getToolCalls() { return toolCalls; }
    }

    private Integer toInteger(Object val) {
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) try { return Integer.parseInt((String) val); } catch (Exception e) {}
        return null;
    }

    public static class ToolCall {
        private final String callId;
        private final String toolName;
        private final Map<String, Object> params;
        private String accessDecision;      // ALLOWED / BLOCKED / PERMISSION_DENIED / NOT_FOUND
        private String toolResultSummary;   // 结果摘要（前200字符）
        private String fallbackReason;      // null=成功, 非null=失败原因

        public ToolCall(String callId, String toolName, Map<String, Object> params) {
            this.callId = callId;
            this.toolName = toolName;
            this.params = params;
        }
        public String getId() { return callId; }
        public String getCallId() { return callId; }
        public String getToolName() { return toolName; }
        public Map<String, Object> getParams() { return params; }
        public String getAccessDecision() { return accessDecision; }
        public void setAccessDecision(String v) { this.accessDecision = v; }
        public String getToolResultSummary() { return toolResultSummary; }
        public void setToolResultSummary(String v) { this.toolResultSummary = v; }
        public String getFallbackReason() { return fallbackReason; }
        public void setFallbackReason(String v) { this.fallbackReason = v; }
    }
}
