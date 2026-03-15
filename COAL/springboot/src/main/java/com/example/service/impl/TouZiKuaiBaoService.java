package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.entity.Danwei;
import com.example.entity.TongjiCzcptouzikuaibao;
import com.example.mapper.DanweiMapper;
import com.example.mapper.TongjiCzcptouzikuaibaoMapper;
import com.example.utils.CumulativeUtils;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 产能投资快报服务类
 */
@Service
public class TouZiKuaiBaoService {

    @Resource
    private DanweiMapper danweiMapper;

    @Resource
    private TongjiCzcptouzikuaibaoMapper tongjiMapper;

    @Resource
    private PermissionService permissionService;

    // 汉字数字映射
    private static final String[] CHINESE_NUMBERS = { "", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十" };

    // 煤业公司ID
    private static final int MEIYE_GONGSI_ID = 2;
    // 兖矿集团公司ID（审批层级计算的根节点）
    private static final int YANKUANG_JITUAN_ID = 1;

    /**
     * 单位节点类（包含序号和层级信息）
     */
    @Data
    public static class DanweiNode {
        private Danwei danwei;
        private String xuhao;
        private Integer level;
        private TongjiCzcptouzikuaibao data; // 可能是实际数据或累加数据
        private boolean aggregated; // 是否为累加数据
        private List<DanweiNode> children; // 子节点
    }

    /**
     * 获取投资快报数据
     * 
     * @param danweiId 单位ID
     * @param nianfen  年份
     * @param yuefen   月份
     * @param leibie   类别（本月/累计）
     * @return 报表数据列表
     */
    public List<Map<String, Object>> getReportData(Integer danweiId, Integer nianfen, Integer yuefen, String leibie) {
        // 【优化】一次性获取所有单位
        List<Danwei> allUnits = danweiMapper.selectList(null);

        // 【优化】一次性批量加载所有报表数据
        Map<Integer, TongjiCzcptouzikuaibao> dataCache = loadAllReportData(nianfen, yuefen, leibie);

        // 构建单位树（使用批量加载的数据缓存）
        DanweiNode rootNode = buildUnitTreeWithData(danweiId, allUnits, nianfen, yuefen, leibie, 0, "", dataCache);

        // 展开树为列表
        List<Map<String, Object>> result = new ArrayList<>();

        if (rootNode != null) {
            // 首先添加总计行（选中单位本身的数据）
            Map<String, Object> totalRow = new HashMap<>();
            totalRow.put("xuhao", "");
            totalRow.put("danweiMingcheng", "总计");
            totalRow.put("level", 0);
            totalRow.put("data", rootNode.getData());
            totalRow.put("aggregated", rootNode.isAggregated());
            totalRow.put("isTotal", true);
            result.add(totalRow);

            // 然后添加所有子节点
            if (rootNode.getChildren() != null) {
                for (DanweiNode child : rootNode.getChildren()) {
                    flattenTree(child, result);
                }
            }
        }

        return result;
    }

    /**
     * 【优化】批量加载所有报表数据（一次查询）
     * 
     * @param nianfen 年份
     * @param yuefen  月份
     * @param leibie  类别（本月/累计）
     * @return 单位ID -> 数据记录的映射
     */
    private Map<Integer, TongjiCzcptouzikuaibao> loadAllReportData(Integer nianfen, Integer yuefen, String leibie) {
        QueryWrapper<TongjiCzcptouzikuaibao> wrapper = new QueryWrapper<>();
        wrapper.eq("NianFen", nianfen);

        if ("累计".equals(leibie)) {
            // 累计模式：查询该年份中 <= 指定月份的所有记录
            wrapper.le("YueFen", yuefen);
            wrapper.orderByDesc("YueFen"); // 按月份降序
        } else {
            // 本月模式：精确匹配月份
            if (yuefen != null) {
                wrapper.eq("YueFen", yuefen);
            }
        }

        List<TongjiCzcptouzikuaibao> allData = tongjiMapper.selectList(wrapper);

        // 转换为 Map<DanweiId, Data>，累计模式下保留每个单位最新月份的记录
        return allData.stream().collect(Collectors.toMap(
                TongjiCzcptouzikuaibao::getDanweiid,
                Function.identity(),
                (existing, replacement) -> {
                    // 保留月份较大的记录
                    if (existing.getYuefen() == null)
                        return replacement;
                    if (replacement.getYuefen() == null)
                        return existing;
                    return existing.getYuefen() >= replacement.getYuefen() ? existing : replacement;
                }));
    }

    /**
     * 递归构建单位树并加载/累加数据
     */
    private DanweiNode buildUnitTreeWithData(Integer parentId, List<Danwei> allUnits,
            Integer nianfen, Integer yuefen, String leibie,
            int level, String parentXuhao, Map<Integer, TongjiCzcptouzikuaibao> dataCache) {
        // 找到当前单位
        Danwei currentUnit = allUnits.stream()
                .filter(u -> u.getId().intValue() == parentId)
                .findFirst()
                .orElse(null);

        if (currentUnit == null) {
            return null;
        }

        // 创建节点
        DanweiNode node = new DanweiNode();
        node.setDanwei(currentUnit);
        node.setLevel(level);
        node.setXuhao(parentXuhao); // 设置序号
        node.setChildren(new ArrayList<>());

        // 找到所有直接子单位
        List<Danwei> children = allUnits.stream()
                .filter(u -> u.getShangjidanweiid() != null && u.getShangjidanweiid().equals(parentId))
                .collect(Collectors.toList());

        // 递归构建子树
        int childIndex = 1;
        for (Danwei child : children) {
            String childXuhao = generateXuhao(level + 1, childIndex, parentXuhao);
            DanweiNode childNode = buildUnitTreeWithData(
                    child.getId().intValue(),
                    allUnits,
                    nianfen,
                    yuefen,
                    leibie,
                    level + 1,
                    childXuhao,
                    dataCache);

            if (childNode != null) {
                node.getChildren().add(childNode);
                childIndex++;
            }
        }

        // 【优化】直接从批量加载的缓存中获取数据（不再单独查询数据库）
        TongjiCzcptouzikuaibao actualData = dataCache.get(currentUnit.getId().intValue());

        // 判断是否有实际数据或子节点
        boolean hasActualData = actualData != null;
        boolean hasChildren = !node.getChildren().isEmpty();

        if (hasActualData) {
            // 有实际数据，直接使用
            node.setData(actualData);
            node.setAggregated(false);
        } else if (hasChildren) {
            // 没有实际数据但有子节点，累加子节点数据
            TongjiCzcptouzikuaibao aggregatedData = aggregateChildrenData(node.getChildren());
            node.setData(aggregatedData);
            node.setAggregated(true);
        } else {
            // 既没有实际数据也没有子节点，返回null（不显示此节点）
            return null;
        }

        return node;
    }

    /**
     * 【优化】累加子节点数据（使用工具类）
     */
    private TongjiCzcptouzikuaibao aggregateChildrenData(List<DanweiNode> children) {
        TongjiCzcptouzikuaibao aggregated = new TongjiCzcptouzikuaibao();

        for (DanweiNode child : children) {
            if (child.getData() != null) {
                // 使用 CumulativeUtils 工具类进行累加
                CumulativeUtils.addNumericFields(aggregated, child.getData());
            }
        }

        return aggregated;
    }

    /**
     * 将树展平为列表
     */
    private void flattenTree(DanweiNode node, List<Map<String, Object>> result) {
        if (node == null) {
            return;
        }

        // 添加当前节点
        Map<String, Object> row = new HashMap<>();
        row.put("xuhao", node.getXuhao());
        row.put("danweiMingcheng", node.getDanwei().getMingcheng());
        row.put("level", node.getLevel());
        row.put("data", node.getData());
        row.put("aggregated", node.isAggregated());
        result.add(row);

        // 递归添加子节点
        for (DanweiNode child : node.getChildren()) {
            flattenTree(child, result);
        }
    }

    /**
     * 导出Excel - HTML表格格式，与正确版本完全一致
     */
    public byte[] exportExcel(Integer danweiId, Integer nianfen, Integer yuefen, String leibie) throws IOException {
        List<Map<String, Object>> data = getReportData(danweiId, nianfen, yuefen, leibie);
        Danwei selectedUnit = danweiMapper.selectById(danweiId);
        String unitName = selectedUnit != null ? selectedUnit.getMingcheng() : "兖矿集团公司";
        boolean isLeiji = "累计".equals(leibie);

        StringBuilder html = new StringBuilder();
        // 表格开始
        html.append("<table id=\"infoPlace\" style=\"display: block; font-size: 12px; border-width: 0px 0px 0.5px; border-top-style: initial; border-top-color: initial; border-left-style: initial; border-left-color: white; border-bottom-style: solid; border-bottom-color: black; border-right-style: initial; border-right-color: white; width: 125cm;\" class=\"tbList\" cellpadding=\"0\" cellspacing=\"0\" frame=\"void\">\r\n");

        // ===== thead =====
        html.append("                <thead>\r\n");

        // 第1行：标题
        html.append("                    <tr>\r\n");
        html.append("                        <th colspan=\"45\" nowrap=\"nowrap\" align=\"center\" style=\"font-size: xx-large;border: 0px; \">\r\n");
        html.append("                            <span id=\"BiaoTi\">产值、主要产品产量及固定资产投资快报</span>\r\n");
        html.append("                        </th>\r\n");
        html.append("                    </tr>\r\n");

        // 第2行：编制单位和日期
        html.append("                    <tr>\r\n");
        html.append("                        <th colspan=\"2\" nowrap=\"nowrap\" align=\"left\" style=\"border: 0px; font-size: 13px;\">\r\n");
        html.append("                            <span id=\"DanWei\">编制单位：").append(unitName).append("</span>\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th colspan=\"40\" nowrap=\"nowrap\" align=\"center\" style=\"border: 0px; font-size: 13px;\">\r\n");
        html.append("                            <span id=\"lbNianYue\">").append(nianfen).append("年").append(yuefen != null ? yuefen : 1).append("月（").append(leibie).append("）</span>\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th colspan=\"1\" nowrap=\"nowrap\" align=\"right\" style=\"border: 0px; font-size: 13px;\">\r\n");
        html.append("                        </th>\r\n");
        html.append("                    </tr>\r\n");

        // 第3行：表头第一行（含rowspan和colspan合并）
        html.append("                    <tr>\r\n");
        html.append("                        <th rowspan=\"2\" nowrap=\"nowrap\" style=\"height: 2cm;width:0.8cm;border-top:0.03cm solid #000;border-left-width: 0;border-left-color: white;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            \r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th rowspan=\"2\" nowrap=\"nowrap\" style=\"width:2cm;border-top:0.03cm solid #000;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            单位名称\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th colspan=\"3\" nowrap=\"nowrap\" style=\"height: 1cm;border-top:0.03cm solid #000;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                           经营总值(万元)\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th rowspan=\"2\" nowrap=\"nowrap\" style=\"width:1cm;border-top:0.03cm solid #000;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            新产品<br>\r\n");
        html.append("                            值价<br>\r\n");
        html.append("                            (万元)\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th colspan=\"2\" nowrap=\"nowrap\" style=\"border-top:0.03cm solid #000;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                          工业销售产值（万元）\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th colspan=\"34\" nowrap=\"nowrap\" style=\"border-top:0.03cm solid #000;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            主要产品产量\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th rowspan=\"2\" nowrap=\"nowrap\" style=\"height: 1cm; width:1cm;border-top:0.03cm solid #000;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            企业用电量<br>（万千瓦时）\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th colspan=\"2\" nowrap=\"nowrap\" style=\"border-top:0.03cm solid #000;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            固定资产投资（万元）\r\n");
        html.append("                        </th>\r\n");
        html.append("                       \r\n");
        html.append("                    </tr>\r\n");

        // 第4行：表头第二行（子列）
        html.append("                    <tr>\r\n");
        // 经营总值子列
        html.append("                        <th nowrap=\"nowrap\" style=\"width:1.3cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                           计\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th nowrap=\"nowrap\" style=\"width:1.5cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            工业产值<br>\r\n");
        html.append("                            （现价）\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th nowrap=\"nowrap\" style=\"width:1.3cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            其他产值、<br>\r\n");
        html.append("                            营业额\r\n");
        html.append("                        </th>\r\n");
        // 工业销售产值子列
        html.append("                        <th nowrap=\"nowrap\" style=\"width:1.1cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                           计\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th nowrap=\"nowrap\" style=\"width:1.1cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            其中：出口<br>\r\n");
        html.append("                            交货值\r\n");
        html.append("                        </th>\r\n");

        // 34个产品子列
        appendProductHeader(html, "1.5cm", "原煤<br>\r\n                            （吨）");
        appendProductHeader(html, "1.5cm", "精煤<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "尿素<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "甲醇<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "醋酸<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "焦炭<br>\r\n                              （吨）");
        appendProductHeader(html, "1.1cm", "醋酸<br>\r\n                            乙酯<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "醋酸<br>\r\n                            丁酯<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "醋酐<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "碳酸<br>\r\n                             二甲<br>\r\n                             酯（吨）");
        appendProductHeader(html, "1.1cm", "合成<br>\r\n                            氨（吨）");
        appendProductHeader(html, "1.1cm", "丁醇<br>\r\n                              （吨）");
        appendProductHeader(html, "1.1cm", "聚甲<br>\r\n                             醛<br>\r\n                             （吨）");
        appendProductHeader(html, "1.1cm", "改质<br>\r\n                            沥青<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "蒽油\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "复合<br>\r\n                            肥（吨）");
        appendProductHeader(html, "1.1cm", "铝锭<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "碳素<br>\r\n                              制品（吨）");
        appendProductHeader(html, "1.1cm", "铝铸<br>\r\n                            材（吨）");
        appendProductHeader(html, "1.1cm", "铝挤<br>\r\n                            压材<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "发电量<br>\r\n                            （万千<br>\r\n                            瓦时）");
        appendProductHeader(html, "1.1cm", "皮带<br>\r\n                             运输<br>\r\n                             机（台）");
        appendProductHeader(html, "1.1cm", "输送<br>\r\n                            带（<br>\r\n                            M2）");
        appendProductHeader(html, "1.1cm", "电缆<br>\r\n                              （米）");
        appendProductHeader(html, "1.1cm", "液压<br>\r\n                            支架<br>\r\n                            （架）");
        appendProductHeader(html, "1.1cm", "刮板<br>\r\n                             运输<br>\r\n                             机（台）");
        appendProductHeader(html, "1.1cm", "高岭<br>\r\n                            土（吨）");
        html.append("                        \r\n");
        appendProductHeader(html, "1.1cm", "轻柴<br>\r\n                            油（吨）");
        appendProductHeader(html, "1.1cm", "重柴<br>\r\n                            油（吨）");
        appendProductHeader(html, "1.1cm", "石脑<br>\r\n                            油（吨）");
        appendProductHeader(html, "1.1cm", "液化<br>\r\n                            石油气");
        appendProductHeader(html, "1.1cm", "硫磺<br>\r\n                            （吨）");
        appendProductHeader(html, "1.1cm", "硫酸<br>\r\n                            铵（吨）");
        appendProductHeader(html, "1.1cm", "其他<br>\r\n                            产品");

        // 固定资产投资子列
        html.append("\r\n");
        html.append("                         <th nowrap=\"nowrap\" style=\"width:1.1cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            合计\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th nowrap=\"nowrap\" style=\"width:1.1cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            技术<br>\r\n");
        html.append("                            改造\r\n");
        html.append("                        </th>\r\n");
        html.append("                    </tr>\r\n");
        html.append("                </thead>\r\n");

        // ===== tbody =====
        html.append("                <tbody id=\"infoList\">\r\n");

        for (Map<String, Object> row : data) {
            TongjiCzcptouzikuaibao t = (TongjiCzcptouzikuaibao) row.get("data");
            String xuhao = row.get("xuhao") != null ? row.get("xuhao").toString() : "";
            String mingcheng = row.get("danweiMingcheng").toString();

            html.append("<tr style=\"background-color: rgb(255, 255, 255);\">");

            // 序号（无左边框）
            html.append("<td align=\"center\" style=\"height:0.7cm;border-bottom: 0.03cm solid black;border-left-width: 0;border-left-color: white;\">").append(xuhao).append("</td>");
            // 单位名称
            appendDataCell(html, mingcheng);
            // 经营总值
            appendDataCell(html, fmtDec(isLeiji ? t.getJingyingzongzhihejileiji() : t.getJingyingzongzhiheji()));
            appendDataCell(html, fmtDec(isLeiji ? t.getGongyechanzhileiji() : t.getGongyechanzhi()));
            appendDataCell(html, fmtDec(isLeiji ? t.getQitachanzhileiji() : t.getQitachanzhi()));
            // 新产品值价
            appendDataCell(html, fmtDec(isLeiji ? t.getXinchanpinjiazhileiji() : t.getXinchanpinjiazhi()));
            // 工业销售产值
            appendDataCell(html, fmtDec(isLeiji ? t.getGyxsczhejileiji() : t.getGyxsczheji()));
            appendDataCell(html, fmtDec(isLeiji ? t.getChukoujiaohuozhileiji() : t.getChukoujiaohuozhi()));

            // 34个产品
            appendDataCell(html, fmtLng(isLeiji ? t.getYuanmeileiji() : t.getYuanmei()));
            appendDataCell(html, fmtLng(isLeiji ? t.getJingmeileiji() : t.getJingmei()));
            appendDataCell(html, fmtLng(isLeiji ? t.getNiaosuleiji() : t.getNiaosu()));
            appendDataCell(html, fmtLng(isLeiji ? t.getJiachunleiji() : t.getJiachun()));
            appendDataCell(html, fmtLng(isLeiji ? t.getCusuanleiji() : t.getCusuan()));
            appendDataCell(html, fmtLng(isLeiji ? t.getJiaotanleiji() : t.getJiaotan()));
            appendDataCell(html, fmtLng(isLeiji ? t.getCusuanyizhileiji() : t.getCusuanyizhi()));
            appendDataCell(html, fmtLng(isLeiji ? t.getCusuandingzhileiji() : t.getCusuandingzhi()));
            appendDataCell(html, fmtLng(isLeiji ? t.getCuganleiji() : t.getCugan()));
            appendDataCell(html, fmtLng(isLeiji ? t.getTansuanerjiazhileiji() : t.getTansuanerjiazhi()));
            appendDataCell(html, fmtLng(isLeiji ? t.getHechenganleiji() : t.getHechengan()));
            appendDataCell(html, fmtLng(isLeiji ? t.getDingchunleiji() : t.getDingchun()));
            appendDataCell(html, fmtLng(isLeiji ? t.getJujiaquanleiji() : t.getJujiaquan()));
            appendDataCell(html, fmtLng(isLeiji ? t.getGaizhiliqingleiji() : t.getGaizhiliqing()));
            appendDataCell(html, fmtLng(isLeiji ? t.getEnyouleiji() : t.getEnyou()));
            appendDataCell(html, fmtLng(isLeiji ? t.getFuhefeileiji() : t.getFuhefei()));
            appendDataCell(html, fmtLng(isLeiji ? t.getLvdingleiji() : t.getLvding()));
            appendDataCell(html, fmtLng(isLeiji ? t.getTansuzhipinleiji() : t.getTansuzhipin()));
            appendDataCell(html, fmtLng(isLeiji ? t.getLvzhucaileiji() : t.getLvzhucai()));
            appendDataCell(html, fmtLng(isLeiji ? t.getLvjiyacaileiji() : t.getLvjiyacai()));
            appendDataCell(html, fmtLng(isLeiji ? t.getFadianliangleiji() : t.getFadianliang()));
            appendDataCell(html, fmtLng(isLeiji ? t.getPidaiyunshujileiji() : t.getPidaiyunshuji()));
            appendDataCell(html, fmtLng(isLeiji ? t.getShusongdaileiji() : t.getShusongdai()));
            appendDataCell(html, fmtLng(isLeiji ? t.getDianlanleiji() : t.getDianlan()));
            appendDataCell(html, fmtLng(isLeiji ? t.getYeyazhijialeiji() : t.getYeyazhijia()));
            appendDataCell(html, fmtLng(isLeiji ? t.getGuabanyunshujileiji() : t.getGuabanyunshuji()));
            appendDataCell(html, fmtLng(isLeiji ? t.getGaolingtuleiji() : t.getGaolingtu()));
            appendDataCell(html, fmtLng(isLeiji ? t.getQingchaiiyouleiji() : t.getQingchaiyou()));
            appendDataCell(html, fmtLng(isLeiji ? t.getZhongchaiyouleiji() : t.getZhongchaiyou()));
            appendDataCell(html, fmtLng(isLeiji ? t.getShinaoyouleiji() : t.getShinaoyou()));
            appendDataCell(html, fmtLng(isLeiji ? t.getYehuashiyouqileiji() : t.getYehuashiyouqi()));
            appendDataCell(html, fmtLng(isLeiji ? t.getLiuhuangleiji() : t.getLiuhuang()));
            appendDataCell(html, fmtLng(isLeiji ? t.getLiuhuanganleiji() : t.getLiuhuangan()));
            appendDataCell(html, fmtLng(isLeiji ? t.getQitachanpinleiji() : t.getQitachanpin()));

            // 企业用电量
            appendDataCell(html, fmtLng(isLeiji ? t.getQiyeyongdianliangleiji() : t.getQiyeyongdianliang()));
            // 固定资产投资
            appendDataCell(html, fmtDec(isLeiji ? t.getGdzctzhejileiji() : t.getGdzctzheji()));
            appendDataCell(html, fmtDec(isLeiji ? t.getJishugaizaoleiji() : t.getJishugaizao()));

            html.append("</tr>");
        }

        html.append("\r\n                </tbody>\r\n");
        html.append("            </table>");

        return html.toString().getBytes("UTF-8");
    }

    /**
     * 添加产品表头单元格
     */
    private void appendProductHeader(StringBuilder html, String width, String content) {
        html.append("                        <th nowrap=\"nowrap\" style=\"width:").append(width)
            .append(";border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            ").append(content).append("\r\n");
        html.append("                        </th>\r\n");
    }

    /**
     * 添加数据单元格（居中对齐，带左边框和下边框）
     */
    private void appendDataCell(StringBuilder html, String value) {
        html.append("<td align=\"center\" style=\"height:0.7cm;border-bottom: 0.03cm solid black;border-left: 0.03cm solid black;\">")
            .append(value != null ? value : "").append("</td>");
    }

    /**
     * BigDecimal格式化：null或0显示为空
     */
    private String fmtDec(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) return "";
        return value.stripTrailingZeros().toPlainString();
    }

    /**
     * Long格式化：null或0显示为空
     */
    private String fmtLng(Long value) {
        if (value == null || value == 0) return "";
        return String.valueOf(value);
    }

    /**
     * 生成序号
     * 一级: 一、二、三
     * 二级: 1、2、3
     * 三级: 1.1、2.1、3.1
     * 四级: 1.1.1、2.1.1
     */
    private String generateXuhao(int level, int index, String parentXuhao) {
        if (level == 1) {
            // 一级：汉字数字
            return CHINESE_NUMBERS[Math.min(index, CHINESE_NUMBERS.length - 1)];
        } else if (level == 2) {
            // 二级：阿拉伯数字
            return String.valueOf(index);
        } else if (level == 3) {
            // 三级：父序号.数字
            return parentXuhao + "." + index;
        } else {
            // 四级及以上：继续拼接
            return parentXuhao + "." + index;
        }
    }

    /**
     * 获取所有单位（用于前端下拉选择）
     */
    public List<Danwei> getAllUnits() {
        return danweiMapper.selectList(null);
    }

    /**
     * 获取用户可访问的单位列表（基于权限过滤）
     * 委托给 PermissionService 处理
     * 
     * @param danweiBianma 用户的单位编码
     * @param roleid       用户的角色ID（1=管理员，拥有所有权限）
     * @return 可访问的单位列表
     */
    public List<Danwei> getAccessibleUnits(String danweiBianma, Integer roleid) {
        return permissionService.getAccessibleDanweiList(danweiBianma, roleid);
    }



    // ==================== 基层单位填报功能 ====================

    /**
     * 根据单位编码获取单位信息
     */
    public Danwei getDanweiByBianma(String bianma) {
        QueryWrapper<Danwei> wrapper = new QueryWrapper<>();
        wrapper.eq("BianMa", bianma);
        return danweiMapper.selectOne(wrapper);
    }

    /**
     * 判断是否为基层单位（无下级单位）
     * 
     * @param danweiBianma 单位编码
     * @return true=基层单位可填报, false=非基层单位
     */
    public boolean isBaseUnit(String danweiBianma) {
        Danwei danwei = getDanweiByBianma(danweiBianma);
        if (danwei == null) {
            return false;
        }
        // 查询是否有下级单位
        QueryWrapper<Danwei> wrapper = new QueryWrapper<>();
        wrapper.eq("ShangJiDanWeiID", danwei.getId());
        Long childCount = danweiMapper.selectCount(wrapper);
        return childCount == 0;
    }

    /**
     * 获取单位信息（包含是否为基层单位）
     */
    public Map<String, Object> getUnitInfo(String danweiBianma) {
        Map<String, Object> result = new HashMap<>();
        Danwei danwei = getDanweiByBianma(danweiBianma);
        if (danwei == null) {
            result.put("exists", false);
            return result;
        }

        result.put("exists", true);
        result.put("id", danwei.getId());
        result.put("mingcheng", danwei.getMingcheng());
        result.put("bianma", danwei.getBianma());

        // 检查是否有下级单位
        QueryWrapper<Danwei> wrapper = new QueryWrapper<>();
        wrapper.eq("ShangJiDanWeiID", danwei.getId());
        Long childCount = danweiMapper.selectCount(wrapper);
        result.put("isBaseUnit", childCount == 0);

        return result;
    }

    /**
     * 查询本单位填报列表
     * 
     * @param danweiId 单位ID
     * @param nianfen  年份
     * @param yuefen   月份（可选）
     * @return 填报记录列表
     */
    public List<TongjiCzcptouzikuaibao> getLocalReportList(Integer danweiId, Integer nianfen, Integer yuefen) {
        QueryWrapper<TongjiCzcptouzikuaibao> wrapper = new QueryWrapper<>();
        wrapper.eq("DanWeiID", danweiId);
        wrapper.eq("NianFen", nianfen);
        if (yuefen != null) {
            wrapper.eq("YueFen", yuefen);
        }
        wrapper.orderByDesc("YueFen");
        return tongjiMapper.selectList(wrapper);
    }

    /**
     * 获取上月累计数据（用于计算本月累计值）
     */
    public TongjiCzcptouzikuaibao getLastMonthData(Integer danweiId, Integer nianfen, Integer yuefen) {
        int lastYear = nianfen;
        int lastMonth = yuefen - 1;
        if (lastMonth < 1) {
            lastMonth = 12;
            lastYear = nianfen - 1;
        }

        QueryWrapper<TongjiCzcptouzikuaibao> wrapper = new QueryWrapper<>();
        wrapper.eq("DanWeiID", danweiId);
        wrapper.eq("NianFen", lastYear);
        wrapper.eq("YueFen", lastMonth);
        return tongjiMapper.selectOne(wrapper);
    }

    /**
     * 保存或更新快报记录
     * 自动计算累计值
     */
    public void saveReport(TongjiCzcptouzikuaibao report) {
        // 获取上月数据计算累计值
        TongjiCzcptouzikuaibao lastMonth = getLastMonthData(
                report.getDanweiid(),
                report.getNianfen(),
                report.getYuefen());

        // 计算所有累计字段
        calculateCumulativeValues(report, lastMonth);

        // 保存或更新
        if (report.getId() != null) {
            tongjiMapper.updateById(report);
        } else {
            tongjiMapper.insert(report);
        }
    }

    /**
     * 【优化】计算累计值：使用 CumulativeUtils 工具类
     * 自动匹配 xxx 和 xxxleiji 字段对进行计算
     */
    private void calculateCumulativeValues(TongjiCzcptouzikuaibao current, TongjiCzcptouzikuaibao lastMonth) {
        CumulativeUtils.calculateCumulative(current, lastMonth);
    }

    /**
     * 根据ID获取记录
     */
    public TongjiCzcptouzikuaibao getById(Long id) {
        return tongjiMapper.selectById(id);
    }

    /**
     * 删除记录（仅允许删除待上报或返回修改的）
     */
    public boolean deleteReport(Long id) {
        TongjiCzcptouzikuaibao report = tongjiMapper.selectById(id);
        if (report == null) {
            return false;
        }
        // 检查状态，只有待上报或返回修改的才能删除
        String status = report.getZhuangtai();
        if (!"待上报".equals(status) && !"返回修改".equals(status) && status != null) {
            return false;
        }
        tongjiMapper.deleteById(id);
        return true;
    }

    /**
     * 上报记录
     * 根据单位层级计算审批级别，设置状态为"待审批N"
     */
    public boolean submitReport(Long id) {
        TongjiCzcptouzikuaibao report = tongjiMapper.selectById(id);
        if (report == null) {
            return false;
        }
        // 检查当前状态，只有"待上报"或"返回修改"状态可以上报
        String status = report.getZhuangtai();
        if (!"待上报".equals(status) && !"返回修改".equals(status)) {
            return false;
        }
        // 计算审批层级
        int approvalLevel = calculateApprovalLevel(report.getDanweiid());
        report.setZhuangtai("待审批" + approvalLevel);
        tongjiMapper.updateById(report);
        return true;
    }

    /**
     * 检查是否已存在相同年月的记录
     */
    public boolean existsRecord(Integer danweiId, Integer nianfen, Integer yuefen, Long excludeId) {
        QueryWrapper<TongjiCzcptouzikuaibao> wrapper = new QueryWrapper<>();
        wrapper.eq("DanWeiID", danweiId);
        wrapper.eq("NianFen", nianfen);
        wrapper.eq("YueFen", yuefen);
        if (excludeId != null) {
            wrapper.ne("ID", excludeId);
        }
        return tongjiMapper.selectCount(wrapper) > 0;
    }

    // ==================== 审批流程相关方法 ====================

    /**
     * 计算单位的审批层级
     * 从煤业公司(ID=2)开始计算深度，然后减1
     * 例如：煤矿(深度2) -> 待审批1
     *
     * @param danweiId 单位ID
     * @return 审批层级 (1=待审批1, 2=待审批2, ...)
     */
    public int calculateApprovalLevel(Integer danweiId) {
        int depth = 0;
        Integer currentId = danweiId;

        // 向上遍历直到兖矿集团(id=1)，每级计数+1
        while (currentId != null && currentId != YANKUANG_JITUAN_ID && currentId != 0) {
            Danwei danwei = danweiMapper.selectById(currentId);
            if (danwei == null) {
                break;
            }
            depth++;
            currentId = danwei.getShangjidanweiid();
        }

        // 审批层级 = 遍历深度（最小为1）
        // 例如：转龙湾→鄂尔多斯→子公司→煤业公司 遍历4次 = 待审批4
        return Math.max(1, depth);
    }

    /**
     * 获取单位层级深度（从煤业公司开始计算）
     *
     * @param danweiId 单位ID
     * @return 层级深度
     */
    public int getUnitDepth(Integer danweiId) {
        int depth = 0;
        Integer currentId = danweiId;

        while (currentId != null && currentId != MEIYE_GONGSI_ID) {
            Danwei danwei = danweiMapper.selectById(currentId);
            if (danwei == null) {
                break;
            }
            depth++;
            currentId = danwei.getShangjidanweiid();
        }
        return depth;
    }

    /**
     * 检查操作权限：上级单位可以操作下级记录
     *
     * @param operatorDanweiId 操作者单位ID
     * @param recordDanweiId   记录所属单位ID
     * @return true=有权限
     */
    public boolean canOperateRecord(Integer operatorDanweiId, Integer recordDanweiId) {
        // 同一单位可以操作
        if (operatorDanweiId.equals(recordDanweiId)) {
            return true;
        }

        // 检查操作者是否是记录单位的上级
        Integer currentId = recordDanweiId;
        while (currentId != null) {
            Danwei danwei = danweiMapper.selectById(currentId);
            if (danwei == null) {
                break;
            }
            if (danwei.getShangjidanweiid() != null && danwei.getShangjidanweiid().equals(operatorDanweiId)) {
                return true;
            }
            currentId = danwei.getShangjidanweiid();
        }
        return false;
    }

    /**
     * 审批通过（支持跳级审批，会返回警告信息）
     *
     * @param id               记录ID
     * @param operatorDanweiId 操作者单位ID
     * @param forceApprove     是否强制审批（跳过下级未审批警告）
     * @return 操作结果消息
     */
    public String approveReport(Long id, Integer operatorDanweiId, boolean forceApprove) {
        TongjiCzcptouzikuaibao report = tongjiMapper.selectById(id);
        if (report == null) {
            return "记录不存在";
        }

        String currentStatus = report.getZhuangtai();
        // 检查状态是否为待审批
        if (currentStatus == null || !currentStatus.startsWith("待审批")) {
            return "当前状态不可审批";
        }

        // 获取记录所属单位
        Integer recordDanweiId = report.getDanweiid();

        // 检查操作权限
        if (!canOperateRecord(operatorDanweiId, recordDanweiId)) {
            return "无权限审批此记录";
        }

        // 找到记录单位的直属上级
        Danwei recordDanwei = danweiMapper.selectById(recordDanweiId);
        if (recordDanwei == null) {
            return "记录单位不存在";
        }
        Integer directParentId = recordDanwei.getShangjidanweiid();

        // 如果操作者不是直属上级，需要检查是否跳级
        if (!operatorDanweiId.equals(directParentId) && !forceApprove) {
            // 找出中间未审批的单位
            String pendingUnits = getPendingApprovalUnits(operatorDanweiId, recordDanweiId);
            if (pendingUnits != null && !pendingUnits.isEmpty()) {
                return "SKIP_WARNING:" + pendingUnits;
            }
        }

        // 解析当前审批级别
        try {
            int currentLevel = Integer.parseInt(currentStatus.replace("待审批", ""));
            if (currentLevel <= 1) {
                // 最后一级审批，状态变为审批通过
                report.setZhuangtai("审批通过");
            } else {
                // 还有上级需要审批，降低审批级别
                report.setZhuangtai("待审批" + (currentLevel - 1));
            }
            tongjiMapper.updateById(report);
            return "审批成功";
        } catch (NumberFormatException e) {
            return "状态格式错误";
        }
    }

    /**
     * 获取操作者和记录单位之间未审批的单位名称
     */
    private String getPendingApprovalUnits(Integer operatorDanweiId, Integer recordDanweiId) {
        List<String> pendingUnits = new ArrayList<>();
        Integer currentId = recordDanweiId;

        // 从记录单位向上遍历到操作者单位
        while (currentId != null && !currentId.equals(operatorDanweiId)) {
            Danwei danwei = danweiMapper.selectById(currentId);
            if (danwei == null) {
                break;
            }
            Integer parentId = danwei.getShangjidanweiid();
            if (parentId != null && !parentId.equals(operatorDanweiId)) {
                // 这个父级单位还没轮到审批
                Danwei parentDanwei = danweiMapper.selectById(parentId);
                if (parentDanwei != null) {
                    pendingUnits.add(parentDanwei.getMingcheng());
                }
            }
            currentId = parentId;
        }

        return String.join("、", pendingUnits);
    }

    /**
     * 返回修改（上级退回下级记录）
     *
     * @param id               记录ID
     * @param operatorDanweiId 操作者单位ID
     * @return 操作结果消息
     */
    public String returnForModification(Long id, Integer operatorDanweiId) {
        TongjiCzcptouzikuaibao report = tongjiMapper.selectById(id);
        if (report == null) {
            return "记录不存在";
        }

        // 检查权限
        if (!canOperateRecord(operatorDanweiId, report.getDanweiid())) {
            return "无权限退回此记录";
        }

        String currentStatus = report.getZhuangtai();
        // 检查状态是否为待审批或审批通过（待上报不可退回）
        if (currentStatus == null || "待上报".equals(currentStatus)) {
            return "当前状态不可退回";
        }

        // 状态改为待上报
        report.setZhuangtai("待上报");
        tongjiMapper.updateById(report);
        return "退回成功";
    }

    /**
     * 检查记录是否可编辑（待上报或返回修改状态可编辑）
     *
     * @param id 记录ID
     * @return true=可编辑
     */
    public boolean canEdit(Long id) {
        TongjiCzcptouzikuaibao report = tongjiMapper.selectById(id);
        if (report == null) {
            return false;
        }
        String status = report.getZhuangtai();
        return "待上报".equals(status) || "返回修改".equals(status);
    }

    /**
     * 获取记录的审批状态信息
     */
    public Map<String, Object> getApprovalStatus(Long id) {
        TongjiCzcptouzikuaibao report = tongjiMapper.selectById(id);
        Map<String, Object> result = new HashMap<>();
        if (report == null) {
            result.put("exists", false);
            return result;
        }

        result.put("exists", true);
        result.put("id", report.getId());
        String status = report.getZhuangtai();
        result.put("zhuangtai", status);
        result.put("canEdit", "待上报".equals(status) || "返回修改".equals(status));
        result.put("canApprove", status != null && status.startsWith("待审批"));
        result.put("canReturn", status != null && !status.equals("待上报") && !status.equals("返回修改"));
        return result;
    }
}
