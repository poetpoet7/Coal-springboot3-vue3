package com.example.agent.tool;

import com.example.agent.entity.AgentAuditLog;
import com.example.agent.mapper.AgentAuditLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 准入控制器。
 * 所有 Tool 调用必须经过此类检查。
 * READ 级别直接放行，SUGGEST 级别标记为待确认，WRITE 级别直接拒绝。
 */
@Component
public class AccessGuard {

    @Resource
    private AgentAuditLogMapper auditLogMapper;

    /**
     * 检查 Tool 调用权限并记录审计日志。
     * @return true=放行, false=拒绝
     */
    public boolean checkAndLog(ToolDefinition tool, MapParams params, Integer userId, String sessionId) {
        AccessLevel level = tool.getAccessLevel();

        // 记录审计日志
        AgentAuditLog log = new AgentAuditLog();
        log.setUserId(userId);
        log.setSessionId(sessionId);
        log.setToolName(tool.getName());
        log.setToolParams(params.toString());
        log.setAccessLevel(level.name());
        log.setCreatedAt(LocalDateTime.now());

        switch (level) {
            case READ:
                log.setAccessDecision("ALLOWED");
                auditLogMapper.insert(log);
                return true;

            case SUGGEST:
                // 建议级操作：允许执行，但标记为需要用户确认
                log.setAccessDecision("SUGGESTED");
                auditLogMapper.insert(log);
                return true;

            case WRITE:
            default:
                log.setAccessDecision("BLOCKED");
                auditLogMapper.insert(log);
                return false;
        }
    }

    /**
     * 参数包装类
     */
    public static class MapParams {
        private final java.util.Map<String, Object> params;
        public MapParams(java.util.Map<String, Object> params) { this.params = params; }
        @Override
        public String toString() { return params.toString(); }
    }
}
