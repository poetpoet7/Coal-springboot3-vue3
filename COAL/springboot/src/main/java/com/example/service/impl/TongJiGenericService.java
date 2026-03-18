package com.example.service.impl;

import com.example.entity.Danwei;
import com.example.mapper.DanweiMapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TongJiGenericService {
    @Resource private DanweiMapper danweiMapper;
    @Resource private PermissionService permissionService;
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private NamedParameterJdbcTemplate namedJdbc;

    private static final int ROOT_DANWEI_ID = 1;
    private static final String LEIJI = "累计";
    private static final String BENYUE = "本月";
    private static final Map<String, ModuleConfig> MODULES = new LinkedHashMap<>();

    static {
        reg("jingyingzongzhi", "生产经营总值", "Tb_TongJi_JingYingZongZhi", "Tb_TongJi_JingYingZongZhi_ShenPi", "BeiZhu3");
        reg("chanpinchanxiaocun", "主要工业产品产销存", "Tb_TongJi_ChanPinChanXiaoCun", "Tb_TongJi_ChanXiaoCun_ShenPi", "ZhuangTai", "ChanPinMingCheng");
        reg("chukouchanpin", "主要出口产品情况", "Tb_TongJi_ChuKouChanPin", "Tb_TongJi_ChuKouChanPin_ShenPi", "ZhuangTai", "ChanPinMingCheng");
        reg("zhuyaojishujingji", "主要技术经济指标", "Tb_TongJi_ZhuYaoJiShuJingJiZhiBiao", "Tb_TongJi_ZhuYaoJiShuZhiBiao_ShenPi", "BeiZhu2");
        reg("dianlijishu", "电力企业主营业务技术指标", "Tb_TongJi_DianLiJiShuZhiBiao", "Tb_TongJi_DianLiJiShuZhiBiao_ShenPi", "BeiZhu3");
        reg("huagongyewu", "化工企业主营业务技术指标", "Tb_TongJi_HuaGongYeWuZhiBiao", "Tb_TongJi_HuaGongZhiBiao_ShenPi", "ZhuangTai", "PinZhong");
        reg("feimeilaodonggongzi", "非煤产业劳动工资", "Tb_TongJi_FeiMeiLaoDongGongZi", "Tb_TongJi_FeiMeiGongZi_ShenPi", "ZhuangTai");
    }

    /** 列名 -> 中文标签 映射 */
    private static final Map<String, String> COLUMN_LABELS = new LinkedHashMap<>();
    static {
        // === 经营总值 ===
        COLUMN_LABELS.put("ZongJi", "总计"); COLUMN_LABELS.put("ZongJiLeiJi", "总计");
        COLUMN_LABELS.put("DiErChanYeHeJi", "第二产业合计"); COLUMN_LABELS.put("DiErChanYeHeJiLeiJi", "第二产业合计");
        COLUMN_LABELS.put("ZengJiaZhi", "增加值"); COLUMN_LABELS.put("ZengJiaZhiLeiJi", "增加值");
        COLUMN_LABELS.put("DiYiChanYe", "第一产业"); COLUMN_LABELS.put("DiYiChanYeLeiJi", "第一产业");
        COLUMN_LABELS.put("GongYe", "工业"); COLUMN_LABELS.put("GongYeLeiJi", "工业");
        COLUMN_LABELS.put("MeiTan", "煤炭"); COLUMN_LABELS.put("MeiTanLeiJi", "煤炭");
        COLUMN_LABELS.put("DianLi", "电力"); COLUMN_LABELS.put("DianLiLeiJi", "电力");
        COLUMN_LABELS.put("HuaGong", "化工"); COLUMN_LABELS.put("HuaGongLeiJi", "化工");
        COLUMN_LABELS.put("JianCai", "建材"); COLUMN_LABELS.put("JianCaiLeiJi", "建材");
        COLUMN_LABELS.put("DianZi", "电子"); COLUMN_LABELS.put("DianZiLeiJi", "电子");
        COLUMN_LABELS.put("DiErQiTa", "第二其他"); COLUMN_LABELS.put("DiErQiTaLeiJi", "第二其他");
        COLUMN_LABELS.put("JianZhuYe", "建筑业"); COLUMN_LABELS.put("JianZhuYeLeiJi", "建筑业");
        COLUMN_LABELS.put("DiSanChanYeHeJi", "第三产业合计"); COLUMN_LABELS.put("DiSanChanYeHeJiLeiJi", "第三产业合计");
        COLUMN_LABELS.put("JiaoTongYunShuYe", "交通运输业"); COLUMN_LABELS.put("JiaoTongYunShuYeLeiJi", "交通运输业");
        COLUMN_LABELS.put("ShangMaoLingShou", "商贸零售"); COLUMN_LABELS.put("ShangMaoLingShouLeiJi", "商贸零售");
        COLUMN_LABELS.put("CanYinYe", "餐饮业"); COLUMN_LABELS.put("CanYinYeLeiJi", "餐饮业");
        COLUMN_LABELS.put("LaoWu", "劳务"); COLUMN_LABELS.put("LaoWuLeiJi", "劳务");
        COLUMN_LABELS.put("JinRong", "金融"); COLUMN_LABELS.put("JinRongLeiJi", "金融");
        COLUMN_LABELS.put("SanChanQiTa", "三产其他"); COLUMN_LABELS.put("SanChanQiTaLeiJi", "三产其他");
        COLUMN_LABELS.put("QuanMin", "全民"); COLUMN_LABELS.put("QuanMinLeiJi", "全民");
        COLUMN_LABELS.put("JiTi", "集体"); COLUMN_LABELS.put("JiTiLeiJi", "集体");
        COLUMN_LABELS.put("XingZhiQiTa", "性质其他"); COLUMN_LABELS.put("XingZhiQiTaLeiJi", "性质其他");
        COLUMN_LABELS.put("BYSCJYZZ", "本月生产经营总值"); COLUMN_LABELS.put("LJSCJYZZ", "累计生产经营总值");
        // === 产品产销存 ===
        COLUMN_LABELS.put("ChanPinMingCheng", "产品名称"); COLUMN_LABELS.put("JiLiangDanWei", "计量单位");
        COLUMN_LABELS.put("HeDingNengLi", "核定能力");
        COLUMN_LABELS.put("BenYueChanLiang", "本月产量"); COLUMN_LABELS.put("LeiJiChanLiang", "累计产量");
        COLUMN_LABELS.put("QNTQChanLiang", "去年同期产量"); COLUMN_LABELS.put("BQNChanLiang", "比去年(%)");
        COLUMN_LABELS.put("BenYueXiaoShouLiang", "本月销售量"); COLUMN_LABELS.put("LeiJiXiaoShouLiang", "累计销售量");
        COLUMN_LABELS.put("ZiYongXiaoShouLiang", "自用销售量"); COLUMN_LABELS.put("LJZYXiaoShouLiang", "累计自用销售量");
        COLUMN_LABELS.put("ChuKouXiaoShouLiang", "出口销售量"); COLUMN_LABELS.put("LJCKXiaoShouLiang", "累计出口销售量");
        COLUMN_LABELS.put("QiChuKuCunLiang", "期初库存量"); COLUMN_LABELS.put("QiMoKuCunLiang", "期末库存量");
        COLUMN_LABELS.put("BenQiPingJunJiaGe", "本期平均价格");
        // === 出口产品 ===
        COLUMN_LABELS.put("BenYueShengChanLiang", "本月生产量"); COLUMN_LABELS.put("LeiJiShengChanLiang", "累计生产量");
        COLUMN_LABELS.put("BenYueChuKouLiang", "本月出口量"); COLUMN_LABELS.put("LeiJiChuKouLiang", "累计出口量");
        COLUMN_LABELS.put("ChuKouChuangHui", "出口创汇"); COLUMN_LABELS.put("ChuKouFangXiang", "出口方向");
        // === 主要技术经济指标 ===
        COLUMN_LABELS.put("FeiMeiZengJiaZhi", "非煤增加值"); COLUMN_LABELS.put("FeiMeiZengJiaZhiLeiJi", "非煤增加值");
        COLUMN_LABELS.put("YingYeShouRuHeJi", "营业收入合计"); COLUMN_LABELS.put("YingYeShouRuHeJiLeiJi", "营业收入合计");
        COLUMN_LABELS.put("FeiMeiWaiXiao", "非煤外销"); COLUMN_LABELS.put("FeiMeiWaiXiaoLeiJi", "非煤外销");
        COLUMN_LABELS.put("DiErHeJi", "第二合计"); COLUMN_LABELS.put("DiErHeJiLeiJi", "第二合计");
        COLUMN_LABELS.put("FeiMeiGongYe", "非煤工业"); COLUMN_LABELS.put("FeiMeiGongYeLeiJi", "非煤工业");
        COLUMN_LABELS.put("DiSanChanYe", "第三产业"); COLUMN_LABELS.put("DiSanChanYeLeiJi", "第三产业");
        COLUMN_LABELS.put("LiShuiHeJi", "利税合计"); COLUMN_LABELS.put("LiShuiHeJiLeiJi", "利税合计");
        COLUMN_LABELS.put("LiRun", "利润"); COLUMN_LABELS.put("LiRunLeiJi", "利润");
        COLUMN_LABELS.put("FeiMeiLiRun", "非煤利润"); COLUMN_LABELS.put("FeiMeiLiRenLeiJi", "非煤利润");
        COLUMN_LABELS.put("ShuiJin", "税金"); COLUMN_LABELS.put("ShuiJinLeiJi", "税金");
        COLUMN_LABELS.put("FeiMeiShuiJin", "非煤税金"); COLUMN_LABELS.put("FeiMeiShuiJinLeiJi", "非煤税金");
        COLUMN_LABELS.put("CYRYLDBaoChou", "从业人员劳动报酬"); COLUMN_LABELS.put("CYRYLDBaoChouLeiJi", "从业人员劳动报酬");
        COLUMN_LABELS.put("QYJZXiaoLv", "企业净资产效率"); COLUMN_LABELS.put("QYJZXiaoLvLeiJi", "企业净资产效率");
        COLUMN_LABELS.put("PJCYRenYuan", "平均从业人员"); COLUMN_LABELS.put("PJCYRenYuanLeiJi", "平均从业人员");
        COLUMN_LABELS.put("BenYueZongJi", "本月总计"); COLUMN_LABELS.put("ZhuYingYeWu", "主营业务"); COLUMN_LABELS.put("ZhuYingYeWuLeiJi", "主营业务");
        // === 电力技术指标 ===
        COLUMN_LABELS.put("FaDianLiang", "发电量"); COLUMN_LABELS.put("FaDianLiangLeiJi", "发电量");
        COLUMN_LABELS.put("GongDianLiangHeJi", "供电量合计"); COLUMN_LABELS.put("GongDianLiangHeJiLeiJi", "供电量合计");
        COLUMN_LABELS.put("KuangYongDianLiang", "矿用电量"); COLUMN_LABELS.put("KuangYongDianLiangLeiJi", "矿用电量");
        COLUMN_LABELS.put("ShangWangDianLiang", "上网电量"); COLUMN_LABELS.put("ShangWangDianLiangLeiJi", "上网电量");
        COLUMN_LABELS.put("GongReLiang", "供热量"); COLUMN_LABELS.put("GongReLiangLeiJi", "供热量");
        COLUMN_LABELS.put("MeiNiLiang", "煤泥量"); COLUMN_LABELS.put("MeiNiLiangLeiJi", "煤泥量");
        COLUMN_LABELS.put("MeiGanShiLiang", "煤矸石量"); COLUMN_LABELS.put("MeiGanShiLiangLeiJi", "煤矸石量");
        COLUMN_LABELS.put("XiZhongMeiLiang", "洗中煤量"); COLUMN_LABELS.put("XiZhongMeiLiangLeiJi", "洗中煤量");
        COLUMN_LABELS.put("FaDianChengBen", "发电成本"); COLUMN_LABELS.put("FaDianChengBenLeiJi", "发电成本");
        COLUMN_LABELS.put("DianLiangShouRu", "电量收入"); COLUMN_LABELS.put("DianLiangShouRuLeiJi", "电量收入");
        COLUMN_LABELS.put("LiRui", "利润"); COLUMN_LABELS.put("LiRuiLeiJi", "利润");
        // === 化工业务指标 ===
        COLUMN_LABELS.put("PinZhong", "品种");
        COLUMN_LABELS.put("BenYueWC", "本月完成"); COLUMN_LABELS.put("LeiJiWC", "累计完成");
        COLUMN_LABELS.put("ShangNianTongQi", "上年同期"); COLUMN_LABELS.put("BiShangNian", "比上年(%)");
        COLUMN_LABELS.put("SCChengBenDanWei", "成本单位");
        COLUMN_LABELS.put("SCChengBenBenYue", "生产成本(本月)"); COLUMN_LABELS.put("SCChengBenLeiJi", "生产成本(累计)");
        COLUMN_LABELS.put("KaiGongLvDanWei", "开工率单位");
        COLUMN_LABELS.put("KaiGongLvBenYue", "开工率(本月)"); COLUMN_LABELS.put("KaiGongLvLeiJi", "开工率(累计)");
        // === 非煤劳动工资 ===
        COLUMN_LABELS.put("RenShuHeJi", "人数合计");
        COLUMN_LABELS.put("YiChanYe", "一产业"); COLUMN_LABELS.put("ErChanYe", "二产业");
        COLUMN_LABELS.put("ErChanYeGongYe", "二产业工业"); COLUMN_LABELS.put("ErChanYeJianZhuYe", "二产业建筑业");
        COLUMN_LABELS.put("SanChanYe", "三产业");
        COLUMN_LABELS.put("BenYuePingJunRenShu", "本月平均人数");
        COLUMN_LABELS.put("BenYueGongZiZongE", "本月工资总额"); COLUMN_LABELS.put("BenYuePingJunGongZi", "本月平均工资");
        COLUMN_LABELS.put("LeiJiPingJunRenShu", "累计平均人数");
        COLUMN_LABELS.put("LeiJiGongZiZongE", "累计工资总额"); COLUMN_LABELS.put("LeiJiPingJunGongZi", "累计平均工资");
        COLUMN_LABELS.put("NvZhiGong", "女职工"); COLUMN_LABELS.put("GuanLiRenYuan", "管理人员");
        COLUMN_LABELS.put("ZhuanYeRenYuan", "专业人员"); COLUMN_LABELS.put("QiTa", "其他");
        // 通用
        COLUMN_LABELS.put("XuHao", "序号"); COLUMN_LABELS.put("BianMa", "编码");
    }

    private static String getLabel(String colName) { return COLUMN_LABELS.getOrDefault(colName, colName); }

    private static void reg(String key, String name, String table, String approval, String status, String... dims) {
        MODULES.put(key, new ModuleConfig(key, name, table, approval, status, Arrays.asList(dims)));
    }

    private final Map<String, List<ColumnMeta>> colCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> numericColCache = new ConcurrentHashMap<>();

    public ModuleConfig getModule(String key) {
        ModuleConfig c = MODULES.get(key);
        if (c == null) throw new IllegalArgumentException("不支持的模块: " + key);
        return c;
    }

    public Map<String, Object> getMeta(String moduleKey) {
        ModuleConfig m = getModule(moduleKey);
        Map<String, Object> out = new HashMap<>();
        out.put("moduleKey", m.key);
        out.put("moduleName", m.name);
        out.put("statusColumn", m.statusColumn);
        out.put("dimensionColumns", m.dimensionColumns);
        out.put("columns", getColumns(m.tableName));
        out.put("dimensionOptions", loadDimensionOptions(m));
        // 维度列中文标签
        Map<String, String> dimLabels = new HashMap<>();
        for (String d : m.dimensionColumns) dimLabels.put(d, getLabel(d));
        out.put("dimensionLabels", dimLabels);
        return out;
    }

    public List<Danwei> getAllUnits() { return danweiMapper.selectList(null); }
    public List<Danwei> getAccessibleUnits(String bianma, Integer roleid) { return permissionService.getAccessibleDanweiList(bianma, roleid); }

    public boolean isBaseUnit(String bianma) {
        Danwei u = getDanweiByBianma(bianma);
        if (u == null) return false;
        return danweiMapper.selectList(null).stream().noneMatch(d -> Objects.equals(u.getId().intValue(), d.getShangjidanweiid()));
    }

    public Map<String, Object> getUnitInfo(String bianma) {
        Map<String, Object> out = new HashMap<>();
        Danwei d = getDanweiByBianma(bianma);
        if (d == null) { out.put("exists", false); return out; }
        out.put("exists", true); out.put("id", d.getId()); out.put("mingcheng", d.getMingcheng()); out.put("bianma", d.getBianma()); out.put("isBaseUnit", isBaseUnit(bianma));
        return out;
    }

    public List<Map<String, Object>> getLocalList(String moduleKey, Integer danweiId, Integer nianfen, Integer yuefen, Map<String, Object> filters) {
        ModuleConfig m = getModule(moduleKey);
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + q(m.tableName) + " WHERE [DanWeiID]=:danweiId AND [NianFen]=:nianfen");
        p.addValue("danweiId", danweiId); p.addValue("nianfen", nianfen);
        if (yuefen != null) { sql.append(" AND [YueFen]=:yuefen"); p.addValue("yuefen", yuefen); }
        appendDimensions(m, filters, sql, p);
        sql.append(" ORDER BY [YueFen] DESC,[ID] DESC");
        return namedJdbc.queryForList(sql.toString(), p);
    }

    public boolean existsRecord(String moduleKey, Map<String, Object> payload, Long excludeId) {
        ModuleConfig m = getModule(moduleKey);
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM " + q(m.tableName) + " WHERE [DanWeiID]=:d AND [NianFen]=:n AND [YueFen]=:y");
        p.addValue("d", get(payload, "DanWeiID")); p.addValue("n", get(payload, "NianFen")); p.addValue("y", get(payload, "YueFen"));
        for (String dim : m.dimensionColumns) { sql.append(" AND ").append(q(dim)).append("=:").append(dim); p.addValue(dim, get(payload, dim)); }
        if (excludeId != null) { sql.append(" AND [ID]<>:id"); p.addValue("id", excludeId); }
        Integer cnt = namedJdbc.queryForObject(sql.toString(), p, Integer.class);
        return cnt != null && cnt > 0;
    }

    public Map<String, Object> getById(String moduleKey, Long id) {
        ModuleConfig m = getModule(moduleKey);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + q(m.tableName) + " WHERE [ID]=?", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void saveOrUpdate(String moduleKey, Map<String, Object> payload) {
        ModuleConfig m = getModule(moduleKey);
        List<ColumnMeta> cols = getColumns(m.tableName);
        calcCumulative(m, payload, cols);
        Long id = toLong(get(payload, "ID"));
        if (id == null) insert(m, cols, payload); else update(m, cols, payload, id);
    }

    public boolean deleteRecord(String moduleKey, Long id) {
        ModuleConfig m = getModule(moduleKey); Map<String, Object> row = getById(moduleKey, id);
        if (row == null) return false; if (!canEdit(normStatus(get(row, m.statusColumn)))) return false;
        return jdbcTemplate.update("DELETE FROM " + q(m.tableName) + " WHERE [ID]=?", id) > 0;
    }

    public boolean submitRecord(String moduleKey, Long id, Integer operatorId) {
        ModuleConfig m = getModule(moduleKey); Map<String, Object> row = getById(moduleKey, id);
        if (row == null) return false;
        if (!canEdit(normStatus(get(row, m.statusColumn)))) return false;
        Integer danweiId = toInt(get(row, "DanWeiID"));
        String newStatus = "待审批" + calculateApprovalLevel(danweiId);
        jdbcTemplate.update("UPDATE " + q(m.tableName) + " SET " + q(m.statusColumn) + "=? WHERE [ID]=?", newStatus, id);
        recordHistory(m, id, operatorId, "上报数据", null, newStatus);
        return true;
    }

    public String approveRecord(String moduleKey, Long id, Integer operatorDanweiId, boolean forceApprove, Integer operatorId) {
        ModuleConfig m = getModule(moduleKey); Map<String, Object> row = getById(moduleKey, id);
        if (row == null) return "记录不存在";
        String status = normStatus(get(row, m.statusColumn));
        if (status == null || !status.startsWith("待审批")) return "当前状态不可审批";
        Integer recordDanweiId = toInt(get(row, "DanWeiID"));
        if (!canOperateRecord(operatorDanweiId, recordDanweiId)) return "无权限审批此记录";
        Danwei record = danweiMapper.selectById(recordDanweiId);
        Integer directParent = record != null ? record.getShangjidanweiid() : null;
        if (!Objects.equals(operatorDanweiId, directParent) && !forceApprove) {
            String pending = getPendingUnits(operatorDanweiId, recordDanweiId);
            if (!pending.isEmpty()) return "SKIP_WARNING:" + pending;
        }
        int level;
        try { level = Integer.parseInt(status.replace("待审批", "")); } catch (Exception e) { return "状态格式错误"; }
        String newStatus = level <= 1 ? "审批通过" : "待审批" + (level - 1);
        jdbcTemplate.update("UPDATE " + q(m.tableName) + " SET " + q(m.statusColumn) + "=? WHERE [ID]=?", newStatus, id);
        Danwei op = danweiMapper.selectById(operatorDanweiId);
        recordHistory(m, id, operatorId, "审批通过", null, status + "(" + (op == null ? "" : op.getMingcheng()) + ")");
        return "审批成功";
    }

    public String returnForModification(String moduleKey, Long id, Integer operatorDanweiId, Integer operatorId) {
        ModuleConfig m = getModule(moduleKey); Map<String, Object> row = getById(moduleKey, id);
        if (row == null) return "记录不存在";
        Integer recordDanweiId = toInt(get(row, "DanWeiID"));
        if (!canOperateRecord(operatorDanweiId, recordDanweiId)) return "无权限退回此记录";
        String status = normStatus(get(row, m.statusColumn));
        if (status == null || status.isEmpty() || "待上报".equals(status)) return "当前状态不可退回";
        jdbcTemplate.update("UPDATE " + q(m.tableName) + " SET " + q(m.statusColumn) + "='待上报' WHERE [ID]=?", id);
        Danwei op = danweiMapper.selectById(operatorDanweiId);
        recordHistory(m, id, operatorId, "返回修改", null, status + "(" + (op == null ? "" : op.getMingcheng()) + ")");
        return "退回成功";
    }

    public Map<String, Object> getApprovalStatus(String moduleKey, Long id) {
        ModuleConfig m = getModule(moduleKey); Map<String, Object> row = getById(moduleKey, id);
        Map<String, Object> out = new HashMap<>();
        if (row == null) { out.put("exists", false); return out; }
        String status = normStatus(get(row, m.statusColumn));
        out.put("exists", true); out.put("id", id); out.put("zhuangtai", status);
        out.put("canEdit", canEdit(status)); out.put("canApprove", status != null && status.startsWith("待审批"));
        out.put("canReturn", status != null && !status.isEmpty() && !"待上报".equals(status) && !"返回修改".equals(status));
        return out;
    }

    public List<Map<String, Object>> getReportData(String moduleKey, Integer danweiId, Integer nianfen, Integer yuefen, String leibie, Map<String, Object> filters) {
        ModuleConfig m = getModule(moduleKey);
        Map<Integer, Map<String, Object>> dataCache = loadAllReportData(m, nianfen, yuefen, leibie, filters);
        DanweiNode root = buildUnitTreeWithData(m, danweiId, danweiMapper.selectList(null), 0, "", dataCache);
        List<Map<String, Object>> out = new ArrayList<>();
        if (root == null) return out;
        Map<String, Object> total = new HashMap<>();
        total.put("xuhao", ""); total.put("danweiMingcheng", "总计"); total.put("level", 0); total.put("data", root.getData()); total.put("aggregated", root.aggregated); total.put("isTotal", true);
        out.add(total);
        for (DanweiNode c : root.children) flattenTree(c, out);
        return out;
    }

    public byte[] exportExcel(String moduleKey, Integer danweiId, Integer nianfen, Integer yuefen, String leibie, Map<String, Object> filters) {
        ModuleConfig m = getModule(moduleKey);
        List<Map<String, Object>> rows = getReportData(moduleKey, danweiId, nianfen, yuefen, leibie, filters);
        Danwei unit = danweiMapper.selectById(danweiId);
        String unitName = unit != null ? unit.getMingcheng() : "兖矿集团公司";
        boolean isLeiji = LEIJI.equals(leibie);
        String lbSuffix = isLeiji ? "（累计）" : "（本月）";
        String ym = nianfen + "年" + (yuefen == null ? 1 : yuefen) + "月" + lbSuffix;

        // 固定列序和表头由各模块定义
        List<String> dataCols = getModuleExportColumns(moduleKey, leibie);
        int totalCols = dataCols.size() + 2; // +序号+单位名称 (带序号的模块)
        boolean hasDimCols = !m.dimensionColumns.isEmpty(); // chanpinchanxiaocun/chukouchanpin/huagongyewu
        if (hasDimCols) totalCols = dataCols.size() + 1; // 维度列已包含在dataCols中，+单位名称或产品名称

        StringBuilder html = new StringBuilder();
        html.append("<table id='infoPlace' style='display:block;font-size:12px;border-width:0 0 .5px;border-bottom-style:solid;border-bottom-color:black;width:33cm;' cellpadding='0' cellspacing='0' frame='void'>");
        html.append("<thead>");

        // 各模块的自定义表头
        appendModuleHeader(html, moduleKey, unitName, ym, dataCols, totalCols, leibie, filters);

        html.append("</thead><tbody id='infoList'>");

        // 数据行
        for (Map<String, Object> r : rows) {
            @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) r.get("data");
            int level = toInt(r.get("level")) == null ? 0 : toInt(r.get("level"));
            html.append("<tr style='background-color:rgb(255,255,255);'>");

            switch (moduleKey) {
                case "chanpinchanxiaocun":
                    // 无序号列，第一列是产品名称（来自维度或单位名称）
                    for (String c : dataCols) {
                        String val;
                        if ("ChanPinMingCheng".equalsIgnoreCase(c)) val = Objects.toString(r.get("danweiMingcheng"), "");
                        else val = fmt(get(data, c));
                        html.append("<td align='center' style='border-bottom:.03cm solid black;border-left:.03cm solid black;'>").append(val).append("</td>");
                    }
                    break;
                case "chukouchanpin":
                    // 序号+单位名称+产品名称+计量单位+数据列
                    html.append("<td align='center' style='height:0.7cm;border-bottom:.03cm solid black;border-left-width:0;'>").append(Objects.toString(r.get("xuhao"), "")).append("</td>");
                    html.append("<td align='center' style='height:0.7cm;border-bottom:.03cm solid black;border-left:.03cm solid black;'>").append(Objects.toString(r.get("danweiMingcheng"), "")).append("</td>");
                    for (String c : dataCols) {
                        String val;
                        if ("ChanPinMingCheng".equalsIgnoreCase(c) || "JiLiangDanWei".equalsIgnoreCase(c))
                            val = Objects.toString(get(data, c), "");
                        else val = fmt(get(data, c));
                        html.append("<td align='center' style='height:0.7cm;border-bottom:.03cm solid black;border-left:.03cm solid black;'>").append(val).append("</td>");
                    }
                    break;
                case "huagongyewu":
                    // 序号+单位名称+品种+计量单位+数据列
                    html.append("<td align='center' style='height:0.7cm;border-bottom:.03cm solid black;border-left-width:0;'>").append(Objects.toString(r.get("xuhao"), "")).append("</td>");
                    html.append("<td align='center' style='height:0.7cm;border-bottom:.03cm solid black;border-left:.03cm solid black;'>").append(Objects.toString(r.get("danweiMingcheng"), "")).append("</td>");
                    for (String c : dataCols) {
                        String val;
                        if ("PinZhong".equalsIgnoreCase(c) || "JiLiangDanWei".equalsIgnoreCase(c) || "SCChengBenDanWei".equalsIgnoreCase(c) || "KaiGongLvDanWei".equalsIgnoreCase(c))
                            val = Objects.toString(get(data, c), "");
                        else val = fmt(get(data, c));
                        html.append("<td align='center' style='height:0.7cm;border-bottom:.03cm solid black;border-left:.03cm solid black;'>").append(val).append("</td>");
                    }
                    break;
                default:
                    // jingyingzongzhi/zhuyaojishujingji/dianlijishu/feimeilaodonggongzi：序号+单位名称+数据列
                    html.append("<td align='center' style='height:0.7cm;border-bottom:.03cm solid black;border-left-width:0;border-left-color:white;'>").append(Objects.toString(r.get("xuhao"), "")).append("</td>");
                    html.append("<td align='center' style='height:0.7cm;border-bottom:.03cm solid black;border-left:.03cm solid black;'>").append(Objects.toString(r.get("danweiMingcheng"), "")).append("</td>");
                    for (String c : dataCols) html.append("<td align='center' style='height:0.7cm;border-bottom:.03cm solid black;border-left:.03cm solid black;'>").append(fmt(get(data, c))).append("</td>");
                    break;
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** 获取各模块导出Excel的固定列序 */
    private List<String> getModuleExportColumns(String moduleKey, String leibie) {
        boolean isLeiji = LEIJI.equals(leibie);
        switch (moduleKey) {
            case "jingyingzongzhi":
                // 经营总值：总计 → 第二产业(合计→工业(计→煤炭)→建筑业) → 第三产业(合计→交通运输业→商贸零售→餐饮业→其他) → 按性质(全民→集体→其他) → 本月生产经营总值
                if (isLeiji) return Arrays.asList("ZongJiLeiJi","DiErChanYeHeJiLeiJi","GongYeLeiJi","MeiTanLeiJi","JianZhuYeLeiJi","DiSanChanYeHeJiLeiJi","JiaoTongYunShuYeLeiJi","ShangMaoLingShouLeiJi","CanYinYeLeiJi","SanChanQiTaLeiJi","QuanMinLeiJi","JiTiLeiJi","XingZhiQiTaLeiJi","LJSCJYZZ");
                else return Arrays.asList("ZongJi","DiErChanYeHeJi","GongYe","MeiTan","JianZhuYe","DiSanChanYeHeJi","JiaoTongYunShuYe","ShangMaoLingShou","CanYinYe","SanChanQiTa","QuanMin","JiTi","XingZhiQiTa","BYSCJYZZ");
            case "chanpinchanxiaocun":
                // 产销存：产品名称→计量单位→核定能力→产量(本月→累计→去年同期→比去年%)→销售量(本月→累计→累计自用→累计出口)→库存(期初→期末)→本期平均价格
                return Arrays.asList("ChanPinMingCheng","JiLiangDanWei","HeDingNengLi","BenYueChanLiang","LeiJiChanLiang","QNTQChanLiang","BQNChanLiang","BenYueXiaoShouLiang","LeiJiXiaoShouLiang","LJZYXiaoShouLiang","LJCKXiaoShouLiang","QiChuKuCunLiang","QiMoKuCunLiang","BenQiPingJunJiaGe");
            case "chukouchanpin":
                // 出口产品：产品名称→计量单位→本月止累计生产量→本月止累计出口量→出口创汇→主要出口方向→备注
                return Arrays.asList("ChanPinMingCheng","JiLiangDanWei","LeiJiShengChanLiang","LeiJiChuKouLiang","ChuKouChuangHui","ChuKouFangXiang");
            case "zhuyaojishujingji":
                // 主要技术经济指标：本月/累计交替排列
                if (isLeiji) return Arrays.asList("ZengJiaZhiLeiJi","FeiMeiZengJiaZhiLeiJi","YingYeShouRuHeJiLeiJi","FeiMeiWaiXiaoLeiJi","DiYiChanYeLeiJi","DiErHeJiLeiJi","GongYeLeiJi","FeiMeiGongYeLeiJi","JianZhuYeLeiJi","DiSanChanYeLeiJi","LiShuiHeJiLeiJi","LiRunLeiJi","FeiMeiLiRenLeiJi","ShuiJinLeiJi","FeiMeiShuiJinLeiJi","CYRYLDBaoChouLeiJi","QYJZXiaoLvLeiJi","PJCYRenYuanLeiJi","ZhuYingYeWuLeiJi");
                else return Arrays.asList("ZengJiaZhi","FeiMeiZengJiaZhi","YingYeShouRuHeJi","FeiMeiWaiXiao","DiYiChanYe","DiErHeJi","GongYe","FeiMeiGongYe","JianZhuYe","DiSanChanYe","LiShuiHeJi","LiRun","FeiMeiLiRun","ShuiJin","FeiMeiShuiJin","CYRYLDBaoChou","QYJZXiaoLv","PJCYRenYuan","BenYueZongJi","ZhuYingYeWu");
            case "dianlijishu":
                // 电力技术指标：本月/累计成对排列
                if (isLeiji) return Arrays.asList("FaDianLiangLeiJi","GongDianLiangHeJiLeiJi","KuangYongDianLiangLeiJi","ShangWangDianLiangLeiJi","GongReLiangLeiJi","MeiNiLiangLeiJi","MeiGanShiLiangLeiJi","XiZhongMeiLiangLeiJi","FaDianChengBenLeiJi","DianLiangShouRuLeiJi","LiRuiLeiJi");
                else return Arrays.asList("FaDianLiang","FaDianLiangLeiJi","GongDianLiangHeJi","GongDianLiangHeJiLeiJi","KuangYongDianLiang","KuangYongDianLiangLeiJi","ShangWangDianLiang","ShangWangDianLiangLeiJi","GongReLiang","GongReLiangLeiJi","MeiNiLiang","MeiNiLiangLeiJi","MeiGanShiLiang","MeiGanShiLiangLeiJi","XiZhongMeiLiang","XiZhongMeiLiangLeiJi","FaDianChengBen","FaDianChengBenLeiJi","DianLiangShouRu","DianLiangShouRuLeiJi","LiRui","LiRuiLeiJi");
            case "huagongyewu":
                // 化工业务指标：品种→计量单位→本月完成→累计完成→上年同期→比上年%→成本单位→成本本月→成本累计→开工率单位→开工率本月→开工率累计
                return Arrays.asList("PinZhong","JiLiangDanWei","BenYueWC","LeiJiWC","ShangNianTongQi","BiShangNian","SCChengBenDanWei","SCChengBenBenYue","SCChengBenLeiJi","KaiGongLvDanWei","KaiGongLvBenYue","KaiGongLvLeiJi");
            case "feimeilaodonggongzi":
                // 非煤劳动工资：人数(合计→一产→二产→工业→建筑→三产)→本月(平均人数→工资总额→平均工资)→累计(平均人数→工资总额→平均工资)→女职工→管理人员→专业人员→全民→集体→其他
                return Arrays.asList("RenShuHeJi","YiChanYe","ErChanYe","ErChanYeGongYe","ErChanYeJianZhuYe","SanChanYe","BenYuePingJunRenShu","BenYueGongZiZongE","BenYuePingJunGongZi","LeiJiPingJunRenShu","LeiJiGongZiZongE","LeiJiPingJunGongZi","NvZhiGong","GuanLiRenYuan","ZhuanYeRenYuan","QuanMin","JiTi","QiTa");
            default:
                return getReportDisplayColumns(getModule(moduleKey), leibie);
        }
    }

    /** 各模块自定义Excel表头HTML */
    private void appendModuleHeader(StringBuilder html, String moduleKey, String unitName, String ym, List<String> dataCols, int totalCols, String leibie, Map<String, Object> filters) {
        // 通用边框样式缩写
        String bt = "border-top:.03cm solid #000;"; String bl = "border-left:.03cm solid black;"; String bb = "border-bottom:.03cm solid black;";
        boolean isLeiji = LEIJI.equals(leibie);
        switch (moduleKey) {
            case "jingyingzongzhi": {
                int nc = 16; // 序号+单位+14数据列
                html.append("<tr><th colspan='").append(nc).append("' align='center' style='font-size:xx-large;border:0;'>生产经营总值</th></tr>");
                html.append("<tr><th colspan='2' align='left' style='border:0;font-size:13px;'>编制单位：").append(unitName).append("</th>");
                html.append("<th colspan='12' align='center' style='border:0;font-size:13px;'>").append(ym).append("</th>");
                html.append("<th colspan='2' align='right' style='border:0;font-size:13px;'>金额单位：万元</th></tr>");
                // 第3行
                html.append("<tr>");
                html.append("<th rowspan='4' style='height:2.5cm;width:0.9cm;").append(bt).append("border-left-width:0;").append(bb).append("'>序号</th>");
                html.append("<th rowspan='4' style='width:3cm;").append(bt).append(bl).append(bb).append("'>单位名称</th>");
                html.append("<th rowspan='4' style='width:2cm;").append(bt).append(bl).append(bb).append("'>总计</th>");
                html.append("<th colspan='9' style='height:0.6cm;").append(bt).append(bl).append(bb).append("'>按产业分生产经营总值</th>");
                html.append("<th colspan='3' style='height:0.6cm;").append(bt).append(bl).append(bb).append("'>按性质分生产经营总值</th>");
                html.append("<th rowspan='4' style='width:1.6cm;").append(bt).append(bl).append(bb).append("'>本月生产<br>经营总值</th>");
                html.append("</tr>");
                // 第4行
                html.append("<tr>");
                html.append("<th colspan='4' style='height:0.7cm;").append(bl).append(bb).append("'>第二产业</th>");
                html.append("<th colspan='5' style='height:0.7cm;").append(bl).append(bb).append("'>第三产业</th>");
                html.append("<th rowspan='3' style='width:1.6cm;").append(bl).append(bb).append("'>全民</th>");
                html.append("<th rowspan='3' style='width:1.6cm;").append(bl).append(bb).append("'>集体</th>");
                html.append("<th rowspan='3' style='width:1.6cm;").append(bl).append(bb).append("'>其他</th>");
                html.append("</tr>");
                // 第5行
                html.append("<tr>");
                html.append("<th rowspan='2' style='width:2cm;").append(bl).append(bb).append("'>合计</th>");
                html.append("<th colspan='2' style='height:0.6cm;").append(bl).append(bb).append("'>工业</th>");
                html.append("<th rowspan='2' style='width:1.6cm;").append(bl).append(bb).append("'>建筑业</th>");
                html.append("<th rowspan='2' style='width:2cm;").append(bl).append(bb).append("'>合计</th>");
                html.append("<th rowspan='2' style='width:1.6cm;").append(bl).append(bb).append("'>交通运输业</th>");
                html.append("<th rowspan='2' style='width:1.6cm;").append(bl).append(bb).append("'>商贸零售</th>");
                html.append("<th rowspan='2' style='width:1.6cm;").append(bl).append(bb).append("'>餐饮业</th>");
                html.append("<th rowspan='2' style='width:1.6cm;").append(bl).append(bb).append("'>其他</th>");
                html.append("</tr>");
                // 第6行
                html.append("<tr>");
                html.append("<th style='width:1.6cm;").append(bl).append(bb).append("'>计</th>");
                html.append("<th style='width:1.6cm;").append(bl).append(bb).append("'>煤炭</th>");
                html.append("</tr>");
                break;
            }
            case "chanpinchanxiaocun": {
                String filterText = "";
                if (filters != null) { Object fp = get(filters, "ChanPinMingCheng"); if (fp != null && !String.valueOf(fp).trim().isEmpty()) filterText = "（" + fp + "）"; else filterText = "（全部）"; }
                int nc = 14;
                html.append("<tr><th colspan='").append(nc).append("' align='center' style='font-size:xx-large;border:0;'>主要工业产品产、销、存</th></tr>");
                html.append("<tr><th colspan='1' align='left' style='border:0;font-size:13px;'>编制单位：").append(unitName).append("</th>");
                html.append("<th colspan='12' align='center' style='border:0;font-size:13px;'>").append(ym).append(filterText).append("</th>");
                html.append("<th colspan='1' align='right' style='border:0;font-size:13px;'>单位：万元</th></tr>");
                // 第3行
                html.append("<tr>");
                html.append("<th rowspan='2' style='height:1.6cm;width:4cm;").append(bt).append("border-left-width:0;").append(bb).append("'>单位(或产品)名称</th>");
                html.append("<th rowspan='2' style='width:1cm;").append(bt).append(bl).append(bb).append("'>计量单位</th>");
                html.append("<th rowspan='2' style='width:2cm;").append(bt).append(bl).append(bb).append("'>设计或核<br>定生产能力</th>");
                html.append("<th colspan='4' style='height:0.8cm;").append(bt).append(bl).append(bb).append("'>产品产量</th>");
                html.append("<th colspan='4' style='height:0.8cm;").append(bt).append(bl).append(bb).append("'>产品销售量</th>");
                html.append("<th colspan='2' style='height:0.8cm;").append(bt).append(bl).append(bb).append("'>库存量</th>");
                html.append("<th rowspan='2' style='width:2cm;").append(bt).append(bl).append(bb).append("'>产品本期<br>平均价格（元）</th>");
                html.append("</tr>");
                // 第4行
                html.append("<tr>");
                html.append("<th style='width:2cm;").append(bl).append(bb).append("'>本月</th>");
                html.append("<th style='width:2cm;").append(bl).append(bb).append("'>累计</th>");
                html.append("<th style='width:2cm;").append(bl).append(bb).append("'>去年同期</th>");
                html.append("<th style='width:2cm;").append(bl).append(bb).append("'>比去年%</th>");
                html.append("<th style='width:2cm;").append(bl).append(bb).append("'>本月</th>");
                html.append("<th style='width:2cm;").append(bl).append(bb).append("'>累计</th>");
                html.append("<th style='width:2cm;").append(bl).append(bb).append("'>累计自用</th>");
                html.append("<th style='width:2cm;").append(bl).append(bb).append("'>累计出口</th>");
                html.append("<th style='width:2cm;").append(bl).append(bb).append("'>期初库存</th>");
                html.append("<th style='width:2cm;").append(bl).append(bb).append("'>期末库存</th>");
                html.append("</tr>");
                break;
            }
            case "chukouchanpin": {
                int nc = 9; // 序号+单位名称+产品名称+计量单位+累计生产量+累计出口量+出口创汇+出口方向+备注
                html.append("<tr><th colspan='").append(nc).append("' align='center' style='font-size:xx-large;border:0;'>出口产品情况统计表</th></tr>");
                html.append("<tr><th colspan='2' align='left' style='border:0;font-size:13px;'>编制单位：").append(unitName).append("</th>");
                html.append("<th colspan='5' align='center' style='border:0;font-size:13px;'>").append(ym).append("</th>");
                html.append("<th colspan='2' align='right' style='border:0;font-size:13px;'></th></tr>");
                // 单行表头
                html.append("<tr>");
                html.append("<th style='height:1.5cm;width:1cm;").append(bt).append("border-left-width:0;").append(bb).append("'>序号</th>");
                html.append("<th style='width:5.5cm;").append(bt).append(bl).append(bb).append("'>单位名称</th>");
                html.append("<th style='width:2.5cm;").append(bt).append(bl).append(bb).append("'>产品名称</th>");
                html.append("<th style='width:2.5cm;").append(bt).append(bl).append(bb).append("'>计量单位</th>");
                html.append("<th style='width:2.5cm;").append(bt).append(bl).append(bb).append("'>本月止<br>累计生产量</th>");
                html.append("<th style='width:2.5cm;").append(bt).append(bl).append(bb).append("'>本月止<br>累计出口量</th>");
                html.append("<th style='width:2.5cm;").append(bt).append(bl).append(bb).append("'>出口创汇<br>(人民币万元)</th>");
                html.append("<th style='width:7cm;").append(bt).append(bl).append(bb).append("'>主要出<br>口方向</th>");
                html.append("<th style='width:4cm;").append(bt).append(bl).append(bb).append("'>备注</th>");
                html.append("</tr>");
                break;
            }
            case "zhuyaojishujingji": {
                int nc = dataCols.size() + 2;
                String title = isLeiji ? "主要技术经济指标(累计)" : "主要技术经济指标(本月)";
                html.append("<tr><th colspan='").append(nc).append("' align='center' style='font-size:xx-large;border:0;'>").append(title).append("</th></tr>");
                html.append("<tr><th colspan='2' align='left' style='border:0;font-size:13px;'>编制单位：").append(unitName).append("</th>");
                html.append("<th colspan='").append(nc - 2).append("' align='center' style='border:0;font-size:13px;'>").append(ym).append("</th></tr>");
                // 双行表头
                html.append("<tr>");
                html.append("<th rowspan='2' style='height:1.6cm;width:0.9cm;").append(bt).append("border-left-width:0;").append(bb).append("'>序号</th>");
                html.append("<th rowspan='2' style='width:3cm;").append(bt).append(bl).append(bb).append("'>单位名称</th>");
                if (isLeiji) {
                    String[] labels = {"增加值","非煤增加值","营业收入合计","非煤外销","第一产业","第二合计","工业","非煤工业","建筑业","第三产业","利税合计","利润","非煤利润","税金","非煤税金","从业人员劳动报酬","企业净资产效率","平均从业人员","主营业务"};
                    html.append("<th colspan='").append(labels.length).append("' style='").append(bt).append(bl).append(bb).append("'>累计</th></tr><tr>");
                    for (String l : labels) html.append("<th style='width:1.5cm;").append(bl).append(bb).append("'>").append(l).append("</th>");
                } else {
                    String[] labels = {"增加值","非煤增加值","营业收入合计","非煤外销","第一产业","第二合计","工业","非煤工业","建筑业","第三产业","利税合计","利润","非煤利润","税金","非煤税金","从业人员劳动报酬","企业净资产效率","平均从业人员","本月总计","主营业务"};
                    html.append("<th colspan='").append(labels.length).append("' style='").append(bt).append(bl).append(bb).append("'>本月</th></tr><tr>");
                    for (String l : labels) html.append("<th style='width:1.5cm;").append(bl).append(bb).append("'>").append(l).append("</th>");
                }
                html.append("</tr>");
                break;
            }
            case "dianlijishu": {
                int nc = dataCols.size() + 2;
                html.append("<tr><th colspan='").append(nc).append("' align='center' style='font-size:xx-large;border:0;'>电力企业主营业务技术指标</th></tr>");
                html.append("<tr><th colspan='2' align='left' style='border:0;font-size:13px;'>编制单位：").append(unitName).append("</th>");
                html.append("<th colspan='").append(nc - 2).append("' align='center' style='border:0;font-size:13px;'>").append(ym).append("</th></tr>");
                if (isLeiji) {
                    // 累计模式：单行数据列
                    String[] labels = {"发电量","供电量合计","矿用电量","上网电量","供热量","煤泥量","煤矸石量","洗中煤量","发电成本","电量收入","利润"};
                    html.append("<tr>");
                    html.append("<th rowspan='1' style='height:1.5cm;width:0.9cm;").append(bt).append("border-left-width:0;").append(bb).append("'>序号</th>");
                    html.append("<th rowspan='1' style='width:3cm;").append(bt).append(bl).append(bb).append("'>单位名称</th>");
                    for (String l : labels) html.append("<th style='").append(bt).append(bl).append(bb).append("'>").append(l).append("</th>");
                    html.append("</tr>");
                } else {
                    // 本月模式：3行表头，本月/累计成对
                    html.append("<tr>");
                    html.append("<th rowspan='3' style='height:2.5cm;width:0.9cm;").append(bt).append("border-left-width:0;").append(bb).append("'>序号</th>");
                    html.append("<th rowspan='3' style='width:3cm;").append(bt).append(bl).append(bb).append("'>单位名称</th>");
                    String[] groups = {"发电量","供电量合计","矿用电量","上网电量","供热量","煤泥量","煤矸石量","洗中煤量","发电成本","电量收入","利润"};
                    for (String g : groups) html.append("<th colspan='2' style='").append(bt).append(bl).append(bb).append("'>").append(g).append("</th>");
                    html.append("</tr><tr>");
                    for (int i = 0; i < groups.length; i++) {
                        html.append("<th style='").append(bl).append(bb).append("'>本月</th>");
                        html.append("<th style='").append(bl).append(bb).append("'>累计</th>");
                    }
                    html.append("</tr>");
                }
                break;
            }
            case "huagongyewu": {
                int nc = dataCols.size() + 2;
                html.append("<tr><th colspan='").append(nc).append("' align='center' style='font-size:xx-large;border:0;'>化工企业主营业务技术指标</th></tr>");
                html.append("<tr><th colspan='2' align='left' style='border:0;font-size:13px;'>编制单位：").append(unitName).append("</th>");
                html.append("<th colspan='").append(nc - 2).append("' align='center' style='border:0;font-size:13px;'>").append(ym).append("</th></tr>");
                // 双行表头
                html.append("<tr>");
                html.append("<th rowspan='2' style='height:1.5cm;width:0.9cm;").append(bt).append("border-left-width:0;").append(bb).append("'>序号</th>");
                html.append("<th rowspan='2' style='width:3cm;").append(bt).append(bl).append(bb).append("'>单位名称</th>");
                html.append("<th rowspan='2' style='width:2cm;").append(bt).append(bl).append(bb).append("'>品种</th>");
                html.append("<th rowspan='2' style='width:1cm;").append(bt).append(bl).append(bb).append("'>计量单位</th>");
                html.append("<th colspan='4' style='").append(bt).append(bl).append(bb).append("'>产量</th>");
                html.append("<th colspan='3' style='").append(bt).append(bl).append(bb).append("'>生产成本</th>");
                html.append("<th colspan='3' style='").append(bt).append(bl).append(bb).append("'>开工率</th>");
                html.append("</tr><tr>");
                html.append("<th style='").append(bl).append(bb).append("'>本月完成</th>");
                html.append("<th style='").append(bl).append(bb).append("'>累计完成</th>");
                html.append("<th style='").append(bl).append(bb).append("'>上年同期</th>");
                html.append("<th style='").append(bl).append(bb).append("'>比上年(%)</th>");
                html.append("<th style='").append(bl).append(bb).append("'>单位</th>");
                html.append("<th style='").append(bl).append(bb).append("'>本月</th>");
                html.append("<th style='").append(bl).append(bb).append("'>累计</th>");
                html.append("<th style='").append(bl).append(bb).append("'>单位</th>");
                html.append("<th style='").append(bl).append(bb).append("'>本月</th>");
                html.append("<th style='").append(bl).append(bb).append("'>累计</th>");
                html.append("</tr>");
                break;
            }
            case "feimeilaodonggongzi": {
                int nc = 20; // 序号+单位+18数据列
                html.append("<tr><th colspan='").append(nc).append("' align='center' style='font-size:xx-large;border:0;'>非煤产业劳动工资统计表</th></tr>");
                html.append("<tr><th colspan='2' align='left' style='border:0;font-size:13px;'>编制单位：").append(unitName).append("</th>");
                html.append("<th colspan='16' align='center' style='border:0;font-size:13px;'>").append(ym).append("</th>");
                html.append("<th colspan='2' align='right' style='border:0;font-size:13px;'>工资单位：万元</th></tr>");
                // 双行表头
                html.append("<tr>");
                html.append("<th rowspan='2' style='height:1.6cm;width:0.9cm;").append(bt).append("border-left-width:0;").append(bb).append("'>序号</th>");
                html.append("<th rowspan='2' style='width:3cm;").append(bt).append(bl).append(bb).append("'>单位名称</th>");
                html.append("<th colspan='6' style='").append(bt).append(bl).append(bb).append("'>期末人数</th>");
                html.append("<th colspan='3' style='").append(bt).append(bl).append(bb).append("'>本月</th>");
                html.append("<th colspan='3' style='").append(bt).append(bl).append(bb).append("'>累计</th>");
                html.append("<th colspan='3' style='").append(bt).append(bl).append(bb).append("'>参考指标</th>");
                html.append("<th colspan='3' style='").append(bt).append(bl).append(bb).append("'>按所有制分</th>");
                html.append("</tr><tr>");
                html.append("<th style='").append(bl).append(bb).append("'>合计</th>");
                html.append("<th style='").append(bl).append(bb).append("'>一产业</th>");
                html.append("<th style='").append(bl).append(bb).append("'>二产业</th>");
                html.append("<th style='").append(bl).append(bb).append("'>工业</th>");
                html.append("<th style='").append(bl).append(bb).append("'>建筑业</th>");
                html.append("<th style='").append(bl).append(bb).append("'>三产业</th>");
                html.append("<th style='").append(bl).append(bb).append("'>平均人数</th>");
                html.append("<th style='").append(bl).append(bb).append("'>工资总额</th>");
                html.append("<th style='").append(bl).append(bb).append("'>平均工资</th>");
                html.append("<th style='").append(bl).append(bb).append("'>平均人数</th>");
                html.append("<th style='").append(bl).append(bb).append("'>工资总额</th>");
                html.append("<th style='").append(bl).append(bb).append("'>平均工资</th>");
                html.append("<th style='").append(bl).append(bb).append("'>女职工</th>");
                html.append("<th style='").append(bl).append(bb).append("'>管理人员</th>");
                html.append("<th style='").append(bl).append(bb).append("'>专业人员</th>");
                html.append("<th style='").append(bl).append(bb).append("'>全民</th>");
                html.append("<th style='").append(bl).append(bb).append("'>集体</th>");
                html.append("<th style='").append(bl).append(bb).append("'>其他</th>");
                html.append("</tr>");
                break;
            }
            default: {
                // 默认：使用通用单行表头
                html.append("<tr><th colspan='").append(totalCols).append("' align='center' style='font-size:xx-large;border:0;'>").append(getModule(moduleKey).name).append("</th></tr>");
                html.append("<tr><th colspan='2' align='left' style='border:0;font-size:13px;'>编制单位：").append(unitName).append("</th><th colspan='").append(totalCols - 2).append("' align='center' style='border:0;font-size:13px;'>").append(ym).append("</th></tr>");
                html.append("<tr><th style='height:1cm;").append(bt).append(bb).append("'>序号</th><th style='").append(bt).append(bl).append(bb).append("'>单位名称</th>");
                for (String c : dataCols) html.append("<th style='").append(bt).append(bl).append(bb).append("'>").append(getLabel(c)).append("</th>");
                html.append("</tr>");
                break;
            }
        }
    }

    public String buildExportFileName(String moduleKey, Integer nianfen, Integer yuefen, String leibie) {
        String fileName = nianfen + "年" + (yuefen == null ? 1 : yuefen) + "月" + getModule(moduleKey).name + "(" + (leibie == null ? BENYUE : leibie) + ").xls";
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public boolean canOperateRecord(Integer operatorDanweiId, Integer recordDanweiId) {
        if (operatorDanweiId == null || recordDanweiId == null) return false;
        if (Objects.equals(operatorDanweiId, recordDanweiId)) return true;
        Integer cur = recordDanweiId;
        while (cur != null) {
            Danwei d = danweiMapper.selectById(cur);
            if (d == null) break;
            if (Objects.equals(d.getShangjidanweiid(), operatorDanweiId)) return true;
            cur = d.getShangjidanweiid();
        }
        return false;
    }

    public int calculateApprovalLevel(Integer danweiId) {
        int depth = 0; Integer cur = danweiId;
        while (cur != null && cur != ROOT_DANWEI_ID && cur != 0) {
            Danwei d = danweiMapper.selectById(cur); if (d == null) break; depth++; cur = d.getShangjidanweiid();
        }
        return Math.max(1, depth);
    }

    private void calcCumulative(ModuleConfig m, Map<String, Object> payload, List<ColumnMeta> cols) {
        Integer danweiId = toInt(get(payload, "DanWeiID")), nianfen = toInt(get(payload, "NianFen")), yuefen = toInt(get(payload, "YueFen"));
        if (danweiId == null || nianfen == null || yuefen == null) return;
        Map<String, Object> filters = new HashMap<>(); for (String d : m.dimensionColumns) filters.put(d, get(payload, d));
        Map<String, Object> last = getLastMonthData(m, danweiId, nianfen, yuefen, filters);
        Set<String> names = cols.stream().map(c -> c.name).collect(Collectors.toSet());
        for (ColumnMeta c : cols) if (c.cumulative) {
            String base = c.name.substring(0, c.name.length() - 5);
            if (names.contains(base)) put(payload, c.name, toBd(get(payload, base)).add(toBd(last == null ? null : get(last, c.name))));
        }
    }

    public Map<String, Object> getLastMonthData(String moduleKey, Integer danweiId, Integer nianfen, Integer yuefen, Map<String, Object> filters) {
        ModuleConfig m = getModule(moduleKey);
        return getLastMonthData(m, danweiId, nianfen, yuefen, filters);
    }

    private Map<String, Object> getLastMonthData(ModuleConfig m, Integer danweiId, Integer nianfen, Integer yuefen, Map<String, Object> filters) {
        int ly = nianfen, lm = yuefen - 1; if (lm < 1) { lm = 12; ly--; }
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT TOP 1 * FROM " + q(m.tableName) + " WHERE [DanWeiID]=:d AND [NianFen]=:n AND [YueFen]=:y");
        p.addValue("d", danweiId); p.addValue("n", ly); p.addValue("y", lm); appendDimensions(m, filters, sql, p); sql.append(" ORDER BY [ID] DESC");
        List<Map<String, Object>> rows = namedJdbc.queryForList(sql.toString(), p);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void insert(ModuleConfig m, List<ColumnMeta> cols, Map<String, Object> payload) {
        List<String> names = new ArrayList<>(); MapSqlParameterSource p = new MapSqlParameterSource();
        for (ColumnMeta c : cols) { if ("ID".equalsIgnoreCase(c.name) || !contains(payload, c.name)) continue; names.add(c.name); p.addValue(c.name, get(payload, c.name)); }
        String sql = "INSERT INTO " + q(m.tableName) + "(" + names.stream().map(this::q).collect(Collectors.joining(",")) + ") VALUES(" + names.stream().map(s -> ":" + s).collect(Collectors.joining(",")) + ")";
        namedJdbc.update(sql, p);
    }

    private void update(ModuleConfig m, List<ColumnMeta> cols, Map<String, Object> payload, Long id) {
        List<String> sets = new ArrayList<>(); MapSqlParameterSource p = new MapSqlParameterSource();
        for (ColumnMeta c : cols) { if ("ID".equalsIgnoreCase(c.name) || !contains(payload, c.name)) continue; sets.add(q(c.name) + "=:" + c.name); p.addValue(c.name, get(payload, c.name)); }
        p.addValue("ID", id);
        namedJdbc.update("UPDATE " + q(m.tableName) + " SET " + String.join(",", sets) + " WHERE [ID]=:ID", p);
    }

    private void recordHistory(ModuleConfig m, Long id, Integer operatorId, String result, String opinion, String remark) {
        List<ColumnMeta> cols = getColumns(m.approvalTableName);
        String link = cols.stream().map(c -> c.name).filter(n -> "JiHuaID".equalsIgnoreCase(n) || "ShenQingID".equalsIgnoreCase(n)).findFirst().orElse("JiHuaID");
        Map<String, Object> row = new HashMap<>();
        row.put(link, id == null ? null : id.intValue()); row.put("ShenPiRenID", operatorId == null ? 0 : operatorId); row.put("ShenPiShiJian", new Date());
        row.put("ShenPiJieGuo", result); row.put("ShenPiYiJian", opinion); row.put("BeiZhu", remark);
        List<String> names = cols.stream().map(c -> c.name).filter(n -> !"ID".equalsIgnoreCase(n)).filter(n -> contains(row, n)).collect(Collectors.toList());
        MapSqlParameterSource p = new MapSqlParameterSource(); for (String n : names) p.addValue(n, get(row, n));
        namedJdbc.update("INSERT INTO " + q(m.approvalTableName) + "(" + names.stream().map(this::q).collect(Collectors.joining(",")) + ") VALUES(" + names.stream().map(s -> ":" + s).collect(Collectors.joining(",")) + ")", p);
    }

    private String getPendingUnits(Integer operatorDanweiId, Integer recordDanweiId) {
        List<String> pending = new ArrayList<>(); Integer cur = recordDanweiId;
        while (cur != null && !Objects.equals(cur, operatorDanweiId)) {
            Danwei d = danweiMapper.selectById(cur); if (d == null) break; Integer parent = d.getShangjidanweiid();
            if (parent != null && !Objects.equals(parent, operatorDanweiId)) { Danwei pd = danweiMapper.selectById(parent); if (pd != null) pending.add(pd.getMingcheng()); }
            cur = parent;
        }
        return String.join("、", pending);
    }

    private Map<String, List<String>> loadDimensionOptions(ModuleConfig m) {
        Map<String, List<String>> out = new HashMap<>();
        for (String d : m.dimensionColumns) out.put(d, jdbcTemplate.queryForList("SELECT DISTINCT " + q(d) + " FROM " + q(m.tableName) + " WHERE " + q(d) + " IS NOT NULL", String.class));
        return out;
    }

    private Map<Integer, Map<String, Object>> loadAllReportData(ModuleConfig m, Integer nianfen, Integer yuefen, String leibie, Map<String, Object> filters) {
        MapSqlParameterSource p = new MapSqlParameterSource(); StringBuilder sql = new StringBuilder("SELECT * FROM " + q(m.tableName) + " WHERE [NianFen]=:nianfen");
        p.addValue("nianfen", nianfen);
        if (LEIJI.equals(leibie)) { if (yuefen != null) { sql.append(" AND [YueFen] <= :yuefen"); p.addValue("yuefen", yuefen); } }
        else { if (yuefen != null) { sql.append(" AND [YueFen] = :yuefen"); p.addValue("yuefen", yuefen); } }
        appendDimensions(m, filters, sql, p); sql.append(" ORDER BY [DanWeiID] ASC, [YueFen] DESC, [ID] DESC");
        List<Map<String, Object>> rows = namedJdbc.queryForList(sql.toString(), p);
        List<Map<String, Object>> picked;
        if (LEIJI.equals(leibie)) {
            Map<String, Map<String, Object>> latestByUnitDim = new LinkedHashMap<>();
            for (Map<String, Object> r : rows) {
                Integer unitId = toInt(get(r, "DanWeiID")); if (unitId == null) continue;
                String dimKey = m.dimensionColumns.stream().map(d -> Objects.toString(get(r, d), "")).collect(Collectors.joining("|"));
                latestByUnitDim.putIfAbsent(unitId + "|" + dimKey, r);
            }
            picked = new ArrayList<>(latestByUnitDim.values());
        } else picked = rows;

        Map<Integer, Map<String, Object>> byUnit = new HashMap<>();
        Set<String> numericCols = getNumericColumns(m.tableName);
        for (Map<String, Object> r : picked) {
            Integer unitId = toInt(get(r, "DanWeiID")); if (unitId == null) continue;
            Map<String, Object> merged = byUnit.computeIfAbsent(unitId, k -> new HashMap<>());
            mergeRowInto(merged, r, numericCols);
        }
        return byUnit;
    }

    private DanweiNode buildUnitTreeWithData(ModuleConfig m, Integer unitId, List<Danwei> allUnits, int level, String xuhao, Map<Integer, Map<String, Object>> dataCache) {
        Danwei unit = allUnits.stream().filter(u -> Objects.equals(toInt(u.getId()), unitId)).findFirst().orElse(null);
        if (unit == null) return null;
        List<DanweiNode> children = new ArrayList<>();
        List<Danwei> childUnits = allUnits.stream().filter(u -> u.getShangjidanweiid() != null && u.getShangjidanweiid().equals(unitId)).collect(Collectors.toList());
        int idx = 1;
        for (Danwei cu : childUnits) {
            DanweiNode child = buildUnitTreeWithData(m, toInt(cu.getId()), allUnits, level + 1, genXuhao(level + 1, idx++, xuhao), dataCache);
            if (child != null) children.add(child);
        }
        Map<String, Object> data = dataCache.get(unitId);
        boolean hasActual = data != null && !data.isEmpty(), hasChildren = !children.isEmpty();
        if (!hasActual && !hasChildren) return null;
        if (!hasActual) data = aggregateChildrenData(m, children);
        return new DanweiNode(unit, level, xuhao, data, !hasActual, children);
    }

    private Map<String, Object> aggregateChildrenData(ModuleConfig m, List<DanweiNode> children) {
        Map<String, Object> out = new HashMap<>(); Set<String> numeric = getNumericColumns(m.tableName);
        for (DanweiNode c : children) if (c.data != null) mergeRowInto(out, c.data, numeric);
        return out;
    }

    private void flattenTree(DanweiNode n, List<Map<String, Object>> out) {
        if (n == null) return;
        Map<String, Object> row = new HashMap<>();
        row.put("xuhao", n.xuhao); row.put("danweiMingcheng", n.danwei.getMingcheng()); row.put("level", n.level); row.put("data", n.data); row.put("aggregated", n.aggregated);
        out.add(row);
        for (DanweiNode c : n.children) flattenTree(c, out);
    }

    private void mergeRowInto(Map<String, Object> target, Map<String, Object> src, Set<String> numericCols) {
        if (src == null) return;
        for (Map.Entry<String, Object> e : src.entrySet()) {
            String c = e.getKey(); if (isMeta(c)) continue;
            if (numericCols.contains(c.toLowerCase())) put(target, c, toBd(get(target, c)).add(toBd(e.getValue())));
            else if (!contains(target, c)) put(target, c, e.getValue());
        }
    }

    private List<String> getReportDisplayColumns(ModuleConfig m, String leibie) {
        List<String> out = new ArrayList<>();
        for (ColumnMeta c : getColumns(m.tableName)) {
            if (isMeta(c.name)) continue;
            if (LEIJI.equals(leibie)) { if (c.cumulative) out.add(c.name); }
            else { if (!c.cumulative) out.add(c.name); }
        }
        return out;
    }

    private Set<String> getNumericColumns(String tableName) {
        return numericColCache.computeIfAbsent(tableName, t -> getColumns(t).stream().filter(ColumnMeta::isNumeric).map(c -> c.name.toLowerCase()).collect(Collectors.toSet()));
    }

    private List<ColumnMeta> getColumns(String tableName) {
        return colCache.computeIfAbsent(tableName, t -> jdbcTemplate.query(
                "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME=? ORDER BY ORDINAL_POSITION",
                (rs, i) -> new ColumnMeta(rs.getString(1), rs.getString(2), isNum(rs.getString(2)), isStr(rs.getString(2)),
                        rs.getString(1) != null && rs.getString(1).toLowerCase().endsWith("leiji"), getLabel(rs.getString(1))),
                t));
    }

    private void appendDimensions(ModuleConfig m, Map<String, Object> filters, StringBuilder sql, MapSqlParameterSource p) {
        if (filters == null) return;
        for (String d : m.dimensionColumns) {
            Object v = get(filters, d);
            if (v != null && !String.valueOf(v).trim().isEmpty()) { sql.append(" AND ").append(q(d)).append("=:").append(d); p.addValue(d, v); }
        }
    }

    private Danwei getDanweiByBianma(String bianma) {
        if (bianma == null) return null;
        return danweiMapper.selectList(null).stream().filter(d -> bianma.equals(d.getBianma())).findFirst().orElse(null);
    }

    private String genXuhao(int level, int index, String parent) { String[] cn = {"一","二","三","四","五","六","七","八","九","十"}; if (level == 1) return cn[Math.min(index - 1, cn.length - 1)]; if (level == 2) return String.valueOf(index); return parent + "." + index; }
    private boolean isMeta(String c) { if (c == null) return true; String x = c.toLowerCase(); return "id".equals(x) || "danweiid".equals(x) || "nianfen".equals(x) || "yuefen".equals(x) || x.startsWith("zhuangtai") || x.startsWith("beizhu"); }
    private String q(String n) { return "[" + n + "]"; }
    private boolean canEdit(String s) { return s == null || s.isEmpty() || "待上报".equals(s) || "返回修改".equals(s); }
    private String normStatus(Object o) { if (o == null) return null; String s = String.valueOf(o).trim(); return s.isEmpty() ? null : s; }
    private String fmt(Object o) { if (o == null) return ""; if (o instanceof Number) { BigDecimal b = toBd(o).setScale(0, RoundingMode.HALF_UP); return b.compareTo(BigDecimal.ZERO) == 0 ? "" : b.toPlainString(); } return String.valueOf(o); }
    private boolean isNum(String t) { if (t == null) return false; String x = t.toLowerCase(); return x.contains("int") || x.contains("decimal") || x.contains("numeric") || x.contains("float") || x.contains("real") || x.contains("money"); }
    private boolean isStr(String t) { if (t == null) return false; String x = t.toLowerCase(); return x.contains("char") || x.contains("text") || x.contains("nchar") || x.contains("nvarchar"); }
    private Integer toInt(Object o) { if (o == null) return null; if (o instanceof Number) return ((Number) o).intValue(); try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; } }
    private Long toLong(Object o) { if (o == null) return null; if (o instanceof Number) return ((Number) o).longValue(); try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return null; } }
    private BigDecimal toBd(Object o) { if (o == null) return BigDecimal.ZERO; if (o instanceof BigDecimal) return (BigDecimal) o; try { return new BigDecimal(String.valueOf(o).trim()); } catch (Exception e) { return BigDecimal.ZERO; } }
    private Object get(Map<String, Object> map, String key) { if (map == null || key == null) return null; for (Map.Entry<String, Object> e : map.entrySet()) if (e.getKey().equalsIgnoreCase(key)) return e.getValue(); return null; }
    private void put(Map<String, Object> map, String key, Object val) { String old = null; for (String k : map.keySet()) if (k.equalsIgnoreCase(key)) { old = k; break; } map.put(old == null ? key : old, val); }
    private boolean contains(Map<String, Object> map, String key) { return get(map, key) != null || (map != null && map.keySet().stream().anyMatch(k -> k.equalsIgnoreCase(key))); }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ModuleConfig { private String key; private String name; private String tableName; private String approvalTableName; private String statusColumn; private List<String> dimensionColumns; }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ColumnMeta { private String name; private String dataType; private boolean numeric; private boolean stringType; private boolean cumulative; private String label; }
    @Data @NoArgsConstructor @AllArgsConstructor
    private static class DanweiNode { private Danwei danwei; private int level; private String xuhao; private Map<String, Object> data; private boolean aggregated; private List<DanweiNode> children; }
}
