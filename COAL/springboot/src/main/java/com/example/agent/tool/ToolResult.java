package com.example.agent.tool;

/**
 * Tool 调用结果封装
 */
public class ToolResult {

    private final String toolName;
    private final boolean success;
    private final String summary;       // 结果摘要（用于展示和日志）
    private final Object rawData;       // 原始数据（供后续处理）

    public ToolResult(String toolName, boolean success, String summary, Object rawData) {
        this.toolName = toolName;
        this.success = success;
        this.summary = summary;
        this.rawData = rawData;
    }

    public static ToolResult ok(String toolName, String summary, Object rawData) {
        return new ToolResult(toolName, true, summary, rawData);
    }

    public static ToolResult fail(String toolName, String error) {
        return new ToolResult(toolName, false, error, null);
    }

    // ---- Getters ----
    public String getToolName() { return toolName; }
    public boolean isSuccess() { return success; }
    public String getSummary() { return summary; }
    public Object getRawData() { return rawData; }
}
