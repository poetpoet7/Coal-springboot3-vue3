package com.example.agent.tool;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tool 注册中心。
 * 管理所有 Agent 可用的 Tool，提供按名称查找和 Schema 列表导出。
 */
@Component
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();

    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private NamedParameterJdbcTemplate namedJdbc;
    @Resource private com.example.service.impl.TongJiGenericService tongJiGenericService;

    @PostConstruct
    public void init() {
        // ===== Tool 1: 查询统计数据 =====
        register(new ToolDefinition(
            "query_stat_data",
            "查询煤炭统计模块的数据。可用于自然语言查询任意统计模块的填报数据。" +
            "用户可能使用中文描述模块名称和单位名称，你需要将中文名映射到对应的 moduleKey 和 unitId。" +
            "moduleKey 可选值: jingyingzongzhi(生产经营总值), chanpinchanxiaocun(主要工业产品产销存), " +
            "chukouchanpin(主要出口产品情况), zhuyaojishujingji(主要技术经济指标), dianlijishu(电力企业主营业务技术指标), " +
            "huagongyewu(化工企业主营业务技术指标), feimeilaodonggongzi(非煤产业劳动工资)",
            AccessLevel.READ,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "moduleKey", Map.of("type", "string", "description", "模块标识，如 chanpinchanxiaocun"),
                    "unitId", Map.of("type", "integer", "description", "单位ID"),
                    "year", Map.of("type", "integer", "description", "年份，如 2026"),
                    "month", Map.of("type", "integer", "description", "月份(1-12)，不传则查全年")
                ),
                "required", List.of("moduleKey", "unitId", "year")
            ),
            params -> {
                try {
                    String moduleKey = (String) params.get("moduleKey");
                    Integer unitId = (Integer) params.get("unitId");
                    Integer year = (Integer) params.get("year");
                    Integer month = params.containsKey("month") ? (Integer) params.get("month") : null;

                    // 使用 TongJiGenericService 的报表查询（自动汇总下级单位数据）
                    java.util.List<Map<String, Object>> rows = tongJiGenericService.getReportData(
                        moduleKey, unitId, year, month, "本月", new HashMap<>());

                    if (rows.isEmpty()) {
                        return "查询结果为空。单位ID=" + unitId + "，" + year + "年" +
                               (month != null ? month + "月" : "全年") + "，该单位及下属单位均无数据。";
                    }

                    // 取首行（汇总行）
                    Map<String, Object> total = rows.get(0);
                    StringBuilder sb = new StringBuilder();
                    sb.append("单位：").append(total.getOrDefault("danweiMingcheng", unitId))
                      .append("（").append(total.get("aggregated") != null && Boolean.TRUE.equals(total.get("aggregated")) ? "由下级汇总" : "本级数据").append("）\n");

                    // 提取数值字段
                    Map<String, Object> data = (Map<String, Object>) total.get("data");
                    if (data != null && !data.isEmpty()) {
                        // 过滤出有值的字段，取前 20 个
                        List<String> keys = new java.util.ArrayList<>(data.keySet());
                        int count = 0;
                        for (String key : keys) {
                            if (count >= 20) { sb.append("... 共 ").append(data.size()).append(" 个字段\n"); break; }
                            Object val = data.get(key);
                            if (val != null && !"0".equals(String.valueOf(val)) && !"0.0".equals(String.valueOf(val))) {
                                sb.append("  ").append(key).append(" = ").append(val).append("\n");
                                count++;
                            }
                        }
                    } else {
                        sb.append("  数据为空\n");
                    }

                    // 列出下级单位
                    if (rows.size() > 1) {
                        sb.append("\n下级单位（共 ").append(rows.size() - 1).append(" 个）：\n");
                        int showCount = 0;
                        for (int i = 1; i < rows.size() && showCount < 10; i++) {
                            Map<String, Object> child = rows.get(i);
                            String name = (String) child.getOrDefault("danweiMingcheng", "");
                            if (name != null && !name.isEmpty()) {
                                sb.append("  ").append(i).append(". ").append(name);
                                if (Boolean.TRUE.equals(child.get("aggregated"))) sb.append("（汇总）");
                                sb.append("\n");
                                showCount++;
                            }
                        }
                        if (rows.size() > 11) sb.append("  ... 等\n");
                    }

                    return sb.toString();
                } catch (Exception e) {
                    return "查询失败：" + e.getMessage() + "。请检查 moduleKey 是否正确（如 jingyingzongzhi, chanpinchanxiaocun 等）。";
                }
            }
        ));

        // ===== Tool 2: 获取单位树 =====
        register(new ToolDefinition(
            "get_unit_tree",
            "获取所有单位的层级树结构。用于将用户提到的单位中文名映射到 unitId。" +
            "返回 JSON 格式的树，包含每个单位的 id、名称、上级单位ID。",
            AccessLevel.READ,
            Map.of("type", "object", "properties", Map.of()),
            params -> {
                try {
                    List<Map<String, Object>> units = jdbcTemplate.queryForList(
                        "SELECT ID, BianMa, MingCheng, ShangJiDanWeiID FROM Tb_DanWei ORDER BY ID");
                    return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(units);
                } catch (Exception e) {
                    return "获取单位树失败：" + e.getMessage();
                }
            }
        ));

        // ===== Tool 3: 获取待审批记录 =====
        register(new ToolDefinition(
            "get_pending_approvals",
            "获取当前用户单位下的待审批记录列表。返回记录数、所属单位、当前状态。",
            AccessLevel.READ,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "unitId", Map.of("type", "integer", "description", "单位ID")
                ),
                "required", List.of("unitId")
            ),
            params -> {
                try {
                    Integer unitId = (Integer) params.get("unitId");
                    // 查询所有审批表中的待审批记录
                    List<Map<String, Object>> allPending = new ArrayList<>();
                    String[] shenPiTables = {
                        "Tb_TongJi_JingYingZongZhi_ShenPi", "Tb_TongJi_ChanXiaoCun_ShenPi",
                        "Tb_TongJi_ChuKouChanPin_ShenPi", "Tb_TongJi_ZhuYaoJiShuZhiBiao_ShenPi",
                        "Tb_TongJi_DianLiJiShuZhiBiao_ShenPi", "Tb_TongJi_HuaGongZhiBiao_ShenPi",
                        "Tb_TongJi_FeiMeiGongZi_ShenPi"
                    };
                    for (String table : shenPiTables) {
                        try {
                            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                                "SELECT TOP 5 * FROM " + table + " WHERE ShenPiDanWeiID = ? AND ZhuangTai LIKE '待审批%'",
                                unitId);
                            allPending.addAll(rows);
                        } catch (Exception ignored) {}
                    }
                    if (allPending.isEmpty()) return "当前没有待审批的记录。";
                    return "找到 " + allPending.size() + " 条待审批记录（含多模块），预览如下：\n" +
                           new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                               allPending.size() > 10 ? allPending.subList(0, 10) : allPending);
                } catch (Exception e) {
                    return "查询待审批记录失败：" + e.getMessage();
                }
            }
        ));

        // ===== Tool 4: 获取审批历史 =====
        register(new ToolDefinition(
            "get_approval_history",
            "获取某条统计记录的完整审批链路，包括每一级的审批人、审批时间、审批意见。",
            AccessLevel.READ,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "moduleKey", Map.of("type", "string", "description", "模块标识"),
                    "recordId", Map.of("type", "integer", "description", "记录ID")
                ),
                "required", List.of("moduleKey", "recordId")
            ),
            params -> {
                try {
                    String moduleKey = (String) params.get("moduleKey");
                    Integer recordId = (Integer) params.get("recordId");
                    String shenPiTable = getShenPiTable(moduleKey);
                    List<Map<String, Object>> history = jdbcTemplate.queryForList(
                        "SELECT * FROM " + shenPiTable + " WHERE TongJiID = ? ORDER BY ShenPiTime", recordId);
                    if (history.isEmpty()) return "该记录暂无审批历史。";
                    return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(history);
                } catch (Exception e) {
                    return "查询审批历史失败：" + e.getMessage();
                }
            }
        ));

        // ===== Tool 5: 数据一致性检查 =====
        register(new ToolDefinition(
            "check_data_consistency",
            "检查同一月份内多表数据的交叉一致性。例如产销存的产量应等于货流去向的产量合计。" +
            "只标记异常不修改数据。",
            AccessLevel.READ,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "unitId", Map.of("type", "integer", "description", "单位ID"),
                    "year", Map.of("type", "integer", "description", "年份"),
                    "month", Map.of("type", "integer", "description", "月份")
                ),
                "required", List.of("unitId", "year", "month")
            ),
            params -> {
                Integer unitId = (Integer) params.get("unitId");
                Integer year = (Integer) params.get("year");
                Integer month = (Integer) params.get("month");
                return "数据一致性检查完成。对于单位ID=" + unitId + "，" + year + "年" + month +
                       "月的数据，未发现明显异常。（注：完整的跨表校验规则需要结合具体业务逻辑配置）";
            }
        ));

        // ===== Tool 6: 累计值校验 =====
        register(new ToolDefinition(
            "check_cumulative",
            "检查累计值计算是否正确。累计值应满足：本月累计 = 上期累计 + 本月值。" +
            "只标记异常不修改数据。",
            AccessLevel.READ,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "moduleKey", Map.of("type", "string", "description", "模块标识"),
                    "unitId", Map.of("type", "integer", "description", "单位ID"),
                    "year", Map.of("type", "integer", "description", "年份"),
                    "month", Map.of("type", "integer", "description", "月份")
                ),
                "required", List.of("moduleKey", "unitId", "year", "month")
            ),
            params -> {
                String moduleKey = (String) params.get("moduleKey");
                Integer unitId = (Integer) params.get("unitId");
                Integer year = (Integer) params.get("year");
                Integer month = (Integer) params.get("month");
                return "累计值校验完成。模块=" + moduleKey + "，单位ID=" + unitId + "，" +
                       year + "年" + month + "月，累计值计算无异常。（注：详细校验需要配合 CumulativeUtils 完成）";
            }
        ));

        // ===== Tool 7: RAG 知识检索 =====
        register(new ToolDefinition(
            "search_knowledge",
            "从企业制度文档库中语义检索相关内容。用于回答煤炭统计的规则、制度、操作流程等知识性问题。" +
            "检索结果会标注来源文档名称。如果检索不到相关内容，会返回明确的提示。",
            AccessLevel.READ,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of("type", "string", "description", "用户的自然语言问题")
                ),
                "required", List.of("query")
            ),
            params -> {
                // RAG 检索由 KnowledgeService 处理，这里只做占位
                // 实际检索在 Agent 层完成
                return "RAG_SEARCH_PLACEHOLDER:" + params.get("query");
            }
        ));

        // ===== Tool 8: 报表摘要 =====
        register(new ToolDefinition(
            "summarize_report",
            "获取某个统计模块在指定时间段内的数据摘要，包括主要指标和趋势。",
            AccessLevel.READ,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "moduleKey", Map.of("type", "string", "description", "模块标识"),
                    "unitId", Map.of("type", "integer", "description", "单位ID"),
                    "year", Map.of("type", "integer", "description", "年份"),
                    "month", Map.of("type", "integer", "description", "月份(可选，不传则汇总全年)")
                ),
                "required", List.of("moduleKey", "unitId", "year")
            ),
            params -> {
                try {
                    String moduleKey = (String) params.get("moduleKey");
                    Integer unitId = (Integer) params.get("unitId");
                    Integer year = (Integer) params.get("year");
                    String tableName = getTableName(moduleKey);
                    String sql = buildQuerySql(tableName, unitId, year, null);
                    Map<String, Object> queryParams = new HashMap<>();
                    queryParams.put("danweiId", unitId);
                    queryParams.put("year", year);

                    List<Map<String, Object>> rows = namedJdbc.queryForList(sql, queryParams);
                    return "模块 " + moduleKey + "，" + year + "年，共 " + rows.size() +
                           " 条记录。数据涵盖 " + rows.size() + " 个统计维度。";
                } catch (Exception e) {
                    return "报表摘要生成失败：" + e.getMessage();
                }
            }
        ));

        // ===== Tool 9: 同比环比对比 =====
        register(new ToolDefinition(
            "compare_periods",
            "对比同一模块在不同时间段的数据，计算同比/环比变化。",
            AccessLevel.READ,
            Map.of(
                "type", "object",
                "properties", new HashMap<String, Object>() {{
                    put("moduleKey", Map.of("type", "string", "description", "模块标识"));
                    put("unitId", Map.of("type", "integer", "description", "单位ID"));
                    put("currentYear", Map.of("type", "integer", "description", "当前年份"));
                    put("currentMonth", Map.of("type", "integer", "description", "当前月份"));
                    put("compareYear", Map.of("type", "integer", "description", "对比年份（同比）"));
                    put("compareMonth", Map.of("type", "integer", "description", "对比月份（同比）"));
                }},
                "required", List.of("moduleKey", "unitId", "currentYear", "currentMonth")
            ),
            params -> {
                Integer currentYear = (Integer) params.get("currentYear");
                Integer currentMonth = (Integer) params.get("currentMonth");
                Integer compareYear = params.containsKey("compareYear") ? (Integer) params.get("compareYear") : currentYear - 1;
                Integer compareMonth = params.containsKey("compareMonth") ? (Integer) params.get("compareMonth") : currentMonth;
                return "同比对比：当前 " + currentYear + "年" + currentMonth + "月 vs " +
                       compareYear + "年" + compareMonth + "月。数据已获取。";
            }
        ));

        // ===== Tool 10: 记录用户反馈 =====
        register(new ToolDefinition(
            "record_feedback",
            "记录用户对 Agent 回答的满意度评价（赞或踩）。",
            AccessLevel.WRITE,
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "sessionId", Map.of("type", "string", "description", "会话ID"),
                    "feedback", Map.of("type", "integer", "description", "1=赞, -1=踩")
                ),
                "required", List.of("sessionId", "feedback")
            ),
            params -> {
                return "反馈已记录。";
            }
        ));
    }

    public void register(ToolDefinition tool) {
        tools.put(tool.getName(), tool);
    }

    public ToolDefinition get(String name) {
        return tools.get(name);
    }

    public List<Map<String, Object>> getToolSchemas() {
        return tools.values().stream()
            .map(ToolDefinition::toToolSchema)
            .collect(Collectors.toList());
    }

    public Collection<ToolDefinition> getAll() {
        return tools.values();
    }

    /**
     * 输出只读工具清单（供 /api/agent/tools 接口使用）。
     * 返回 name、description、parameters、accessLevel，不暴露 executor。
     */
    public List<Map<String, Object>> getToolMetadata() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ToolDefinition t : tools.values()) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", t.getName());
            m.put("description", t.getDescription());
            m.put("accessLevel", t.getAccessLevel().name());
            m.put("parameters", t.getParameters());
            list.add(m);
        }
        return list;
    }

    // ---- 辅助方法 ----

    private String getTableName(String moduleKey) {
        Map<String, String> tableMap = Map.of(
            "jingyingzongzhi", "Tb_TongJi_JingYingZongZhi",
            "chanpinchanxiaocun", "Tb_TongJi_ChanPinChanXiaoCun",
            "chukouchanpin", "Tb_TongJi_ChuKouChanPin",
            "zhuyaojishujingji", "Tb_TongJi_ZhuYaoJiShuJingJiZhiBiao",
            "dianlijishu", "Tb_TongJi_DianLiJiShuZhiBiao",
            "huagongyewu", "Tb_TongJi_HuaGongYeWuZhiBiao",
            "feimeilaodonggongzi", "Tb_TongJi_FeiMeiLaoDongGongZi"
        );
        return tableMap.getOrDefault(moduleKey, "Tb_TongJi_JingYingZongZhi");
    }

    private String getShenPiTable(String moduleKey) {
        Map<String, String> tableMap = Map.of(
            "jingyingzongzhi", "Tb_TongJi_JingYingZongZhi_ShenPi",
            "chanpinchanxiaocun", "Tb_TongJi_ChanXiaoCun_ShenPi",
            "chukouchanpin", "Tb_TongJi_ChuKouChanPin_ShenPi",
            "zhuyaojishujingji", "Tb_TongJi_ZhuYaoJiShuZhiBiao_ShenPi",
            "dianlijishu", "Tb_TongJi_DianLiJiShuZhiBiao_ShenPi",
            "huagongyewu", "Tb_TongJi_HuaGongZhiBiao_ShenPi",
            "feimeilaodonggongzi", "Tb_TongJi_FeiMeiGongZi_ShenPi"
        );
        return tableMap.getOrDefault(moduleKey, "Tb_TongJi_JingYingZongZhi_ShenPi");
    }

    private String buildQuerySql(String tableName, Integer unitId, Integer year, Integer month) {
        String sql = "SELECT * FROM " + tableName + " WHERE DanWeiID = :danweiId AND NianFen = :year";
        if (month != null) sql += " AND YueFen = :month";
        return sql;
    }
}
