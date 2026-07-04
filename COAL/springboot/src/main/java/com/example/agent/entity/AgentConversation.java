package com.example.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("Tb_Agent_Conversation")
public class AgentConversation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String sessionId;
    private String role;            // user / assistant / tool
    private String content;
    private String toolName;
    private String toolParams;
    private Integer feedback;       // 1=赞, 0=无, -1=踩
    private Integer tokensUsed;
    private LocalDateTime createdAt;
}
