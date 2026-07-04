package com.example.agent.tool;

/**
 * Tool 访问级别。
 * READ    - 只读查询，Agent 可直接执行
 * SUGGEST - 建议级别，Agent 生成建议但需要用户确认
 * WRITE   - 写操作，Agent 永远不可执行
 */
public enum AccessLevel {
    READ,
    SUGGEST,
    WRITE
}
