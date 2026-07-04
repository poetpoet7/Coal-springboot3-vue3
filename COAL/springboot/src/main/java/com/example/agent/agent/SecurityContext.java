package com.example.agent.agent;

import java.util.HashSet;
import java.util.Set;

/**
 * Agent 安全上下文。
 * 从 JWT Token 中解析，全程携带，确保 Agent 不越权访问数据。
 */
public class SecurityContext {

    private final Integer userId;
    private final String userName;
    private final String danweiBianma;       // 用户所属单位编码
    private final String danweiName;         // 用户所属单位名称
    private final Integer danweiId;          // 用户所属单位ID
    private final boolean isAdmin;
    private final Set<Integer> accessibleDanweiIds;  // 可访问的单位ID集合（含本单位和所有下级）

    public SecurityContext(Integer userId, String userName, String danweiBianma, String danweiName,
                           Integer danweiId, boolean isAdmin, Set<Integer> accessibleDanweiIds) {
        this.userId = userId;
        this.userName = userName;
        this.danweiBianma = danweiBianma;
        this.danweiName = danweiName;
        this.danweiId = danweiId;
        this.isAdmin = isAdmin;
        this.accessibleDanweiIds = accessibleDanweiIds != null ? accessibleDanweiIds : new HashSet<>();
    }

    /**
     * 检查目标单位是否在用户可访问范围内。
     */
    public boolean canAccess(Integer targetDanweiId) {
        if (isAdmin) return true;
        if (targetDanweiId == null) return false;
        return accessibleDanweiIds.contains(targetDanweiId);
    }

    /**
     * 检查目标单位是否越权（用户不可访问上级单位数据）。
     */
    public boolean isForbidden(Integer targetDanweiId) {
        return !canAccess(targetDanweiId);
    }

    // ---- Getters ----
    public Integer getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getDanweiBianma() { return danweiBianma; }
    public String getDanweiName() { return danweiName; }
    public Integer getDanweiId() { return danweiId; }
    public boolean isAdmin() { return isAdmin; }
    public Set<Integer> getAccessibleDanweiIds() { return accessibleDanweiIds; }

    /**
     * 生成安全规则文本，注入 System Prompt。
     */
    public String toSystemPromptRules() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 当前用户信息\n");
        sb.append("- 姓名：").append(userName).append("\n");
        sb.append("- 所属单位：").append(danweiName != null ? danweiName : "未关联").append("\n");
        if (isAdmin) {
            sb.append("- 权限：管理员（可访问所有单位数据）\n");
        } else {
            sb.append("- 权限：普通用户（仅可访问本单位及下属单位数据）\n");
            sb.append("- 可访问的单位数量：").append(accessibleDanweiIds.size()).append(" 个\n");
        }
        sb.append("\n## 数据访问边界（严格执行！）\n");
        if (isAdmin) {
            sb.append("你是管理员，可以查询任何单位的数据。\n");
        } else {
            sb.append("你只能查询以上可访问范围内的单位数据。\n");
            sb.append("如果用户要求查询上级单位或其他不可访问的单位，必须拒绝并说明原因。\n");
        }
        sb.append("每次调用 query_stat_data 时，unitId 必须在可访问范围内。\n");
        return sb.toString();
    }
}
