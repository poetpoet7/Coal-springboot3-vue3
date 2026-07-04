package com.example.agent.tool;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Tool 定义 —— 描述 Agent 可调用的一个工具。
 * 每个 Tool 包装一个现有 Service 方法，声明名称、描述、参数 Schema 和访问级别。
 */
public class ToolDefinition {

    private final String name;
    private final String description;
    private final AccessLevel accessLevel;
    private final Map<String, Object> parameters;   // JSON Schema 格式的参数定义
    private final Function<Map<String, Object>, String> executor;  // 实际执行逻辑

    public ToolDefinition(String name, String description, AccessLevel accessLevel,
                          Map<String, Object> parameters,
                          Function<Map<String, Object>, String> executor) {
        this.name = name;
        this.description = description;
        this.accessLevel = accessLevel;
        this.parameters = parameters;
        this.executor = executor;
    }

    /**
     * 执行工具调用。AccessGuard 应在调用前检查权限。
     */
    public String execute(Map<String, Object> params) {
        return executor.apply(params);
    }

    /**
     * 生成 LLM Function Calling 所需的 Tool Schema
     */
    public Map<String, Object> toToolSchema() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters
                )
        );
    }

    // ---- Getters ----
    public String getName() { return name; }
    public String getDescription() { return description; }
    public AccessLevel getAccessLevel() { return accessLevel; }
    public Map<String, Object> getParameters() { return parameters; }
}
