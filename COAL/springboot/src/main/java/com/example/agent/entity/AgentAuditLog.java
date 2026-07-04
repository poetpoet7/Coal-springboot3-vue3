package com.example.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("Tb_Agent_Audit_Log")
public class AgentAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String sessionId;
    private String toolName;
    private String toolParams;
    private String toolResultSummary;
    private String accessLevel;         // READ / WRITE / SUGGEST
    private String accessDecision;      // ALLOWED / BLOCKED
    private String llmModel;
    private Integer durationMs;
    private LocalDateTime createdAt;
}
