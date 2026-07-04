package com.example.agent.controller;

import com.example.agent.agent.CoalAssistantAgent;
import com.example.agent.agent.ConversationService;
import com.example.agent.agent.SecurityContext;
import com.example.entity.UserInfo;
import com.example.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agent 对话 Controller。
 * 提供 SSE 流式聊天接口和反馈接口。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentChatController {

    @Resource private CoalAssistantAgent agent;
    @Resource private ConversationService conversationService;
    @Resource private com.example.service.impl.PermissionService permissionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * SSE 流式聊天接口。
     * 前端通过 EventSource 连接，实时接收 Agent 的思考过程和回答。
     */
    @PostMapping(value = "/chat")
    public SseEmitter chat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);

        executor.execute(() -> {
            try {
                // 从 Token 构建安全上下文（包含用户信息 + 权限范围）
                SecurityContext secCtx = buildSecurityContext(request.getToken());
                if (secCtx == null) {
                    sendEvent(emitter, "error", "未登录或 Token 已过期");
                    emitter.complete();
                    return;
                }

                // 发送思考事件
                sendEvent(emitter, "thought", "正在分析您的问题...");

                // 调用 Agent（传入安全上下文）
                CoalAssistantAgent.AgentResponse response = agent.chat(
                    request.getMessage(), request.getSessionId(), secCtx);

                // 发送工具调用事件
                if (!response.getToolCalls().isEmpty()) {
                    for (CoalAssistantAgent.ToolCall tc : response.getToolCalls()) {
                        Map<String, Object> toolEvent = Map.of(
                            "tool", tc.getToolName(),
                            "params", tc.getParams()
                        );
                        sendEvent(emitter, "tool_call", objectMapper.writeValueAsString(toolEvent));
                    }
                }

                // 发送回答
                sendEvent(emitter, "message", response.getContent());

                // 发送会话 ID
                sendEvent(emitter, "session", response.getSessionId());

                // 完成
                sendEvent(emitter, "done", "completed");
                emitter.complete();

            } catch (Exception e) {
                try {
                    sendEvent(emitter, "error", "处理请求时出错：" + e.getMessage());
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(Throwable -> emitter.completeWithError(Throwable));

        return emitter;
    }

    /**
     * 用户反馈接口。
     */
    @PostMapping("/feedback")
    public Result feedback(@RequestBody FeedbackRequest request) {
        conversationService.saveFeedback(request.getSessionId(), request.getFeedback());
        return Result.success();
    }

    /**
     * 清除会话。
     */
    @PostMapping("/clear-session")
    public Result clearSession(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId != null) {
            conversationService.evictSession(sessionId);
        }
        return Result.success();
    }

    // ---- 辅助方法 ----

    private void sendEvent(SseEmitter emitter, String event, String data) throws IOException {
        emitter.send(SseEmitter.event()
            .name(event)
            .data(data, MediaType.APPLICATION_JSON));
    }

    /**
     * 从 Token 中提取用户 ID。
     */
    /**
     * 从 Token 构建完整安全上下文（用户身份 + 单位 + 权限范围）。
     */
    private SecurityContext buildSecurityContext(String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            String audience = com.auth0.jwt.JWT.decode(token).getAudience().get(0);
            String[] parts = audience.split("@@");
            Integer userId = Integer.parseInt(parts[0]);

            // 查数据库获取用户信息
            com.example.service.UserInfoService userService =
                new com.example.service.UserInfoService();
            // 使用 staticUserInfoService
            UserInfo user = com.example.utils.TokenUtils.getCurrentUser();
            if (user == null) {
                // fallback: 开发环境返回 admin 权限
                return new SecurityContext(1, "admin", "00000000", "兖矿集团公司",
                    1, true, new java.util.HashSet<>());
            }

            boolean isAdmin = user.getRoleid() != null && user.getRoleid() == 1;
            java.util.Set<Integer> accessibleIds = isAdmin
                ? new java.util.HashSet<>()
                : new java.util.HashSet<>(permissionService.getAccessibleDanweiIds(
                    user.getDanweibianma(), user.getRoleid()));

            com.example.entity.Danwei userDanwei = permissionService.getUserDanwei(user.getDanweibianma());

            return new SecurityContext(
                userId,
                user.getUsername(),
                user.getDanweibianma(),
                userDanwei != null ? userDanwei.getMingcheng() : "未知单位",
                userDanwei != null ? userDanwei.getId().intValue() : null,
                isAdmin,
                accessibleIds
            );
        } catch (Exception e) {
            return null;
        }
    }

    // ---- 请求体定义 ----

    public static class ChatRequest {
        private String message;
        private String sessionId;
        private String token;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    public static class FeedbackRequest {
        private String sessionId;
        private int feedback;  // 1=赞, -1=踩
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public int getFeedback() { return feedback; }
        public void setFeedback(int feedback) { this.feedback = feedback; }
    }
}
