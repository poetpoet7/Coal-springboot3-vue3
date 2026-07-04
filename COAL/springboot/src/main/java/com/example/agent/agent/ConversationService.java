package com.example.agent.agent;

import com.example.agent.entity.AgentConversation;
import com.example.agent.mapper.AgentConversationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 会话管理服务。
 * 负责创建、恢复、保存对话会话。
 */
@Service
public class ConversationService {

    @Resource
    private AgentConversationMapper conversationMapper;

    private final java.util.concurrent.ConcurrentHashMap<String, ConversationSession> activeSessions = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 创建新会话。
     */
    public ConversationSession createSession(int maxTokens, int summaryThreshold) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        ConversationSession session = new ConversationSession(sessionId, maxTokens, summaryThreshold);
        activeSessions.put(sessionId, session);
        return session;
    }

    /**
     * 获取或创建会话。
     */
    public ConversationSession getOrCreate(String sessionId, int maxTokens, int summaryThreshold) {
        if (sessionId != null && activeSessions.containsKey(sessionId)) {
            return activeSessions.get(sessionId);
        }
        String newId = sessionId != null ? sessionId : UUID.randomUUID().toString().substring(0, 8);
        ConversationSession session = new ConversationSession(newId, maxTokens, summaryThreshold);
        activeSessions.put(newId, session);
        return session;
    }

    /**
     * 保存消息到数据库（长期存储）。
     */
    public void saveMessage(Integer userId, String sessionId, String role, String content,
                            String toolName, String toolParams, Integer tokensUsed) {
        AgentConversation msg = new AgentConversation();
        msg.setUserId(userId);
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setToolName(toolName);
        msg.setToolParams(toolParams);
        msg.setTokensUsed(tokensUsed);
        msg.setCreatedAt(java.time.LocalDateTime.now());
        conversationMapper.insert(msg);
    }

    /**
     * 保存用户反馈。
     */
    public void saveFeedback(String sessionId, int feedback) {
        // 更新该会话最新一条 assistant 消息的反馈
        List<AgentConversation> messages = conversationMapper.selectList(
            new LambdaQueryWrapper<AgentConversation>()
                .eq(AgentConversation::getSessionId, sessionId)
                .eq(AgentConversation::getRole, "assistant")
                .orderByDesc(AgentConversation::getCreatedAt)
                .last("LIMIT 1")
        );
        if (!messages.isEmpty()) {
            AgentConversation msg = messages.get(0);
            msg.setFeedback(feedback);
            conversationMapper.updateById(msg);
        }
    }

    /**
     * 移除过期会话。
     */
    public void evictSession(String sessionId) {
        activeSessions.remove(sessionId);
    }
}
