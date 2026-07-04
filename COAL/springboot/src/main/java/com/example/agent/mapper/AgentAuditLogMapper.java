package com.example.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.agent.entity.AgentAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentAuditLogMapper extends BaseMapper<AgentAuditLog> {
}
