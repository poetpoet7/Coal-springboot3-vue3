package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.entity.Danwei;
import com.example.entity.ChanPinHuoLiuQuXiang;
import com.example.entity.ChanPinQuXiangShenPiRecord;
import com.example.mapper.DanweiMapper;
import com.example.mapper.ChanPinHuoLiuQuXiangMapper;
import com.example.mapper.ChanPinQuXiangShenPiRecordMapper;
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
 * 产品货流去向服务类
 */
@Service
public class ChanPinHuoLiuQuXiangService {

    @Resource
    private DanweiMapper danweiMapper;

    @Resource
    private ChanPinHuoLiuQuXiangMapper cphlqxMapper;

    @Resource
    private ChanPinQuXiangShenPiRecordMapper chanPinQuXiangShenPiRecordMapper;

    @Resource
    private PermissionService permissionService;

    // 汉字数字映射
    private static final String[] CHINESE_NUMBERS = { "", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十" };

    // 兖矿集团公司ID
    private static final int YANKUANG_JITUAN_ID = 1;

    @Data
    public static class DanweiNode {
        private Danwei danwei;
        private String xuhao;
        private Integer level;
        private ChanPinHuoLiuQuXiang data; 
        private boolean aggregated; 
        private List<DanweiNode> children; 
    }

    public List<Map<String, Object>> getReportData(Integer danweiId, Integer nianfen, Integer yuefen, String leibie, String chanpinmingcheng) {
        List<Danwei> allUnits = danweiMapper.selectList(null);
        Map<Integer, ChanPinHuoLiuQuXiang> dataCache = loadAllReportData(nianfen, yuefen, leibie, chanpinmingcheng);
        DanweiNode rootNode = buildUnitTreeWithData(danweiId, allUnits, nianfen, yuefen, leibie, 0, "", dataCache);
        
        List<Map<String, Object>> result = new ArrayList<>();
        if (rootNode != null) {
            Map<String, Object> totalRow = new HashMap<>();
            totalRow.put("xuhao", "");
            totalRow.put("danweiMingcheng", "总计");
            totalRow.put("level", 0);
            totalRow.put("data", rootNode.getData());
            totalRow.put("aggregated", rootNode.isAggregated());
            totalRow.put("isTotal", true);
            result.add(totalRow);

            if (rootNode.getChildren() != null) {
                for (DanweiNode child : rootNode.getChildren()) {
                    flattenTree(child, result);
                }
            }
        }
        return result;
    }

    private Map<Integer, ChanPinHuoLiuQuXiang> loadAllReportData(Integer nianfen, Integer yuefen, String leibie, String chanpinmingcheng) {
        QueryWrapper<ChanPinHuoLiuQuXiang> wrapper = new QueryWrapper<>();
        wrapper.eq("NianFen", nianfen);
        if (chanpinmingcheng != null && !chanpinmingcheng.isEmpty()) {
            wrapper.eq("ChanPinMingCheng", chanpinmingcheng);
        }

        if ("累计".equals(leibie)) {
            wrapper.le("YueFen", yuefen);
            wrapper.orderByDesc("YueFen");
        } else {
            if (yuefen != null) {
                wrapper.eq("YueFen", yuefen);
            }
        }

        List<ChanPinHuoLiuQuXiang> allData = cphlqxMapper.selectList(wrapper);
        return allData.stream().collect(Collectors.toMap(
                ChanPinHuoLiuQuXiang::getDanweiid,
                Function.identity(),
                (existing, replacement) -> {
                    if (existing.getYuefen() == null) return replacement;
                    if (replacement.getYuefen() == null) return existing;
                    return existing.getYuefen() >= replacement.getYuefen() ? existing : replacement;
                }));
    }

    private DanweiNode buildUnitTreeWithData(Integer parentId, List<Danwei> allUnits,
            Integer nianfen, Integer yuefen, String leibie,
            int level, String parentXuhao, Map<Integer, ChanPinHuoLiuQuXiang> dataCache) {
        
        Danwei currentUnit = allUnits.stream().filter(u -> u.getId().intValue() == parentId).findFirst().orElse(null);
        if (currentUnit == null) return null;

        DanweiNode node = new DanweiNode();
        node.setDanwei(currentUnit);
        node.setLevel(level);
        node.setXuhao(parentXuhao);
        node.setChildren(new ArrayList<>());

        List<Danwei> children = allUnits.stream()
                .filter(u -> u.getShangjidanweiid() != null && u.getShangjidanweiid().equals(parentId))
                .collect(Collectors.toList());

        int childIndex = 1;
        for (Danwei child : children) {
            String childXuhao = generateXuhao(level + 1, childIndex, parentXuhao);
            DanweiNode childNode = buildUnitTreeWithData(child.getId().intValue(), allUnits, nianfen, yuefen, leibie, level + 1, childXuhao, dataCache);
            if (childNode != null) {
                node.getChildren().add(childNode);
                childIndex++;
            }
        }

        ChanPinHuoLiuQuXiang actualData = dataCache.get(currentUnit.getId().intValue());
        boolean hasActualData = actualData != null;
        boolean hasChildren = !node.getChildren().isEmpty();

        if (hasActualData) {
            node.setData(actualData);
            node.setAggregated(false);
        } else if (hasChildren) {
            ChanPinHuoLiuQuXiang aggregatedData = aggregateChildrenData(node.getChildren());
            node.setData(aggregatedData);
            node.setAggregated(true);
        } else {
            return null;
        }

        return node;
    }

    private ChanPinHuoLiuQuXiang aggregateChildrenData(List<DanweiNode> children) {
        ChanPinHuoLiuQuXiang aggregated = new ChanPinHuoLiuQuXiang();
        for (DanweiNode child : children) {
            if (child.getData() != null) {
                CumulativeUtils.addNumericFields(aggregated, child.getData());
            }
        }
        return aggregated;
    }

    private void flattenTree(DanweiNode node, List<Map<String, Object>> result) {
        if (node == null) return;
        Map<String, Object> row = new HashMap<>();
        row.put("xuhao", node.getXuhao());
        row.put("danweiMingcheng", node.getDanwei().getMingcheng());
        row.put("level", node.getLevel());
        row.put("data", node.getData());
        row.put("aggregated", node.isAggregated());
        result.add(row);

        for (DanweiNode child : node.getChildren()) {
            flattenTree(child, result);
        }
    }

    public byte[] exportExcel(Integer danweiId, Integer nianfen, Integer yuefen, String chanpinmingcheng) throws IOException {
        // Excel 导出固定为左边省市区县，右边运输方式（本月及累计），不需要按单位逐层显示
        Map<Integer, ChanPinHuoLiuQuXiang> dataCache = loadAllReportData(nianfen, yuefen, "累计", chanpinmingcheng);
        List<Danwei> allUnits = danweiMapper.selectList(null);
        DanweiNode rootNode = buildUnitTreeWithData(danweiId, allUnits, nianfen, yuefen, "累计", 0, "", dataCache);
        
        ChanPinHuoLiuQuXiang t = (rootNode != null && rootNode.getData() != null) ? rootNode.getData() : new ChanPinHuoLiuQuXiang();
        Danwei selectedUnit = danweiMapper.selectById(danweiId);
        String unitName = selectedUnit != null ? selectedUnit.getMingcheng() : "兖矿集团公司";
        String displayCpName = (chanpinmingcheng != null && !chanpinmingcheng.isEmpty()) ? chanpinmingcheng : "无产品";

        StringBuilder html = new StringBuilder();
        html.append("<table id=\"infoPlace\" style=\"display: block; font-size: 13px; border-width: 0px 0px 0.5px; border-top-style: initial; border-top-color: initial; border-left-style: initial; border-left-color: white; border-bottom-style: solid; border-bottom-color: black; border-right-style: initial; border-right-color: white; width: 33cm;\" class=\"tbList\" cellpadding=\"0\" cellspacing=\"0\" frame=\"void\">\r\n");
        html.append("                <thead>\r\n");
        html.append("                    <tr>\r\n");
        html.append("                        <th colspan=\"6\" nowrap=\"nowrap\" align=\"center\" style=\"font-size: xx-large;border: 0px;\">\r\n");
        html.append("                            <span id=\"BiaoTi\">主要产品（").append(displayCpName).append("）货流去向</span>\r\n");
        html.append("                        </th>\r\n");
        html.append("                    </tr>\r\n");
        html.append("                    <tr>\r\n");
        html.append("                        <th colspan=\"1\" nowrap=\"nowrap\" align=\"left\" style=\"border: 0px; font-size: 13px;\">\r\n");
        html.append("                            <span id=\"DanWei\">编制单位：").append(unitName).append("</span>\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th colspan=\"4\" nowrap=\"nowrap\" align=\"center\" style=\"border: 0px; font-size: 13px;\">\r\n");
        html.append("                            <span id=\"lbNianYue\">").append(nianfen).append("年").append(yuefen != null ? yuefen : 1).append("月</span>\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th colspan=\"1\" nowrap=\"nowrap\" align=\"right\" style=\"border: 0px; font-size: 13px;\">\r\n");
        html.append("                            单位：吨\r\n");
        html.append("                        </th>\r\n");
        html.append("                    </tr>\r\n");
        html.append("                    <tr>\r\n");
        html.append("                        <th colspan=\"3\" nowrap=\"nowrap\" align=\"center\" style=\"height:1cm;border-top:0.03cm solid #000;border-left-width: 0;border-left-color: white;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            销往地区\r\n");
        html.append("                        </th>\r\n");
        html.append("                        <th colspan=\"3\" nowrap=\"nowrap\" align=\"center\" style=\"height:1cm;border-top:0.03cm solid #000;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\">\r\n");
        html.append("                            运输方式\r\n");
        html.append("                        </th>\r\n");
        html.append("                    </tr>\r\n");
        html.append("                    <tr>\r\n");
        html.append("                        <th nowrap=\"nowrap\" style=\"width:6cm;height:1cm;border-left-width: 0;border-left-color: white;border-bottom: 0.03cm solid black;\" align=\"center\"></th>\r\n");
        html.append("                        <th nowrap=\"nowrap\" style=\"width:4cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\" align=\"center\">本月</th>\r\n");
        html.append("                        <th nowrap=\"nowrap\" style=\"width:4cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\" align=\"center\">累计</th>\r\n");
        html.append("                        <th nowrap=\"nowrap\" style=\"width:6cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\" align=\"center\"></th>\r\n");
        html.append("                        <th nowrap=\"nowrap\" style=\"width:4cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\" align=\"center\">本月</th>\r\n");
        html.append("                        <th nowrap=\"nowrap\" style=\"width:4cm;border-left: 0.03cm solid black;border-bottom: 0.03cm solid black;\" align=\"center\">累计</th>\r\n");
        html.append("                    </tr>\r\n");
        html.append("                </thead>\r\n");
        html.append("                <tbody id=\"infoList\">\r\n");

        // 计算合计
        // 销往地区合计 = 山东省内 + 山东省外 + 国外 + 公司自用
        BigDecimal qxBenYue = sum(t.getShandongshengnei(), t.getShandongshengwai(), t.getGuowai(), t.getGongsiziyong());
        BigDecimal qxLeiJi = sum(t.getShandongshengneileiji(), t.getShandongshengwaileiji(), t.getGuowaileiji(), t.getGongsiziyongleiji());
        
        // 运输方式合计 = 铁路运输 + 汽车运输(qicheyunshu) + 内河运输 + 地销 + 自营铁路 + 矿自用
        BigDecimal fsBenYue = sum(t.getTieluyunshu(), t.getQicheyunshu(), t.getNeiheyunshu(), t.getDixiao(), t.getZiyingtielu(), t.getKuangziyong());
        BigDecimal fsLeiJi = sum(t.getTieluyunshuleiji(), t.getQicheyunshuleiji(), t.getNeiheyunshuleiji(), t.getDixiaoleiji(), t.getZiyingtieluleiji(), t.getKuangziyongleiji());

        // Row 1
        appendTwoColsRow(html, "合计", qxBenYue, qxLeiJi, "合计", fsBenYue, fsLeiJi);
        // Row 2
        appendTwoColsRow(html, "山东省内", t.getShandongshengnei(), t.getShandongshengneileiji(), "铁路运输", t.getTieluyunshu(), t.getTieluyunshuleiji());
        // Row 3
        appendTwoColsRow(html, "山东省外", t.getShandongshengwai(), t.getShandongshengwaileiji(), "汽车运输", t.getQicheyunshu(), t.getQicheyunshuleiji());
        // Row 4
        appendTwoColsRow(html, "其中：国外", t.getGuowai(), t.getGuowaileiji(), "内河运输", t.getNeiheyunshu(), t.getNeiheyunshuleiji());
        // Row 5
        appendTwoColsRow(html, "公司自用", t.getGongsiziyong(), t.getGongsiziyongleiji(), "地销", t.getDixiao(), t.getDixiaoleiji());
        // Row 6
        appendTwoColsRow(html, "", null, null, "自营铁路", t.getZiyingtielu(), t.getZiyingtieluleiji());
        // Row 7
        appendTwoColsRow(html, "", null, null, "矿自用", t.getKuangziyong(), t.getKuangziyongleiji());

        html.append("                </tbody>\r\n");
        html.append("            </table>");

        return html.toString().getBytes("UTF-8");
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal val : values) {
            if (val != null) {
                total = total.add(val);
            }
        }
        return total;
    }

    private void appendTwoColsRow(StringBuilder html, String leftName, BigDecimal leftBenYue, BigDecimal leftLeiJi, 
                                  String rightName, BigDecimal rightBenYue, BigDecimal rightLeiJi) {
        html.append("                    <tr style=\"height:32px\">\r\n");
        html.append("                        <td align=\"center\" style=\"border-bottom: 0.03cm solid black;border-left-width: 0;border-left-color: white;\">")
            .append(leftName).append("</td>\r\n");
        html.append("                        <td align=\"center\" style=\"border-bottom: 0.03cm solid black;border-left: 0.03cm solid black;\">")
            .append(fmtDecStr(leftBenYue)).append("</td>\r\n");
        html.append("                        <td align=\"center\" style=\"border-bottom: 0.03cm solid black;border-left: 0.03cm solid black;\">")
            .append(fmtDecStr(leftLeiJi)).append("</td>\r\n");
        
        html.append("                        <td align=\"center\" style=\"border-bottom: 0.03cm solid black;border-left: 0.03cm solid black;\">")
            .append(rightName).append("</td>\r\n");
        html.append("                        <td align=\"center\" style=\"border-bottom: 0.03cm solid black;border-left: 0.03cm solid black;\">")
            .append(fmtDecStr(rightBenYue)).append("</td>\r\n");
        html.append("                        <td align=\"center\" style=\"border-bottom: 0.03cm solid black;border-left: 0.03cm solid black;\">")
            .append(fmtDecStr(rightLeiJi)).append("</td>\r\n");
        html.append("                    </tr>\r\n");
    }

    private String fmtDecStr(BigDecimal value) {
        if (value == null) return "";
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0";
        return value.stripTrailingZeros().toPlainString();
    }

    private String generateXuhao(int level, int index, String parentXuhao) {
        if (level == 1) return CHINESE_NUMBERS[Math.min(index, CHINESE_NUMBERS.length - 1)];
        else if (level == 2) return String.valueOf(index);
        else return parentXuhao + "." + index;
    }

    public List<Danwei> getAllUnits() {
        return danweiMapper.selectList(null);
    }

    public List<Danwei> getAccessibleUnits(String danweiBianma, Integer roleid) {
        return permissionService.getAccessibleDanweiList(danweiBianma, roleid);
    }

    public Danwei getDanweiByBianma(String bianma) {
        QueryWrapper<Danwei> wrapper = new QueryWrapper<>();
        wrapper.eq("BianMa", bianma);
        return danweiMapper.selectOne(wrapper);
    }

    public boolean isBaseUnit(String danweiBianma) {
        Danwei danwei = getDanweiByBianma(danweiBianma);
        if (danwei == null) return false;
        QueryWrapper<Danwei> wrapper = new QueryWrapper<>();
        wrapper.eq("ShangJiDanWeiID", danwei.getId());
        return danweiMapper.selectCount(wrapper) == 0;
    }

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
        QueryWrapper<Danwei> wrapper = new QueryWrapper<>();
        wrapper.eq("ShangJiDanWeiID", danwei.getId());
        result.put("isBaseUnit", danweiMapper.selectCount(wrapper) == 0);
        return result;
    }

    public List<ChanPinHuoLiuQuXiang> getLocalReportList(Integer danweiId, Integer nianfen, Integer yuefen, String chanpinmingcheng) {
        QueryWrapper<ChanPinHuoLiuQuXiang> wrapper = new QueryWrapper<>();
        wrapper.eq("DanWeiID", danweiId);
        wrapper.eq("NianFen", nianfen);
        if (chanpinmingcheng != null && !chanpinmingcheng.isEmpty()) {
            wrapper.eq("ChanPinMingCheng", chanpinmingcheng);
        }
        if (yuefen != null) {
            wrapper.eq("YueFen", yuefen);
        }
        wrapper.orderByDesc("YueFen");
        return cphlqxMapper.selectList(wrapper);
    }

    public ChanPinHuoLiuQuXiang getLastMonthData(Integer danweiId, Integer nianfen, Integer yuefen, String chanpinmingcheng) {
        int lastYear = nianfen;
        int lastMonth = yuefen - 1;
        if (lastMonth < 1) {
            lastMonth = 12;
            lastYear = nianfen - 1;
        }
        QueryWrapper<ChanPinHuoLiuQuXiang> wrapper = new QueryWrapper<>();
        wrapper.eq("DanWeiID", danweiId);
        wrapper.eq("NianFen", lastYear);
        wrapper.eq("YueFen", lastMonth);
        if (chanpinmingcheng != null && !chanpinmingcheng.isEmpty()) {
            wrapper.eq("ChanPinMingCheng", chanpinmingcheng);
        }
        return cphlqxMapper.selectOne(wrapper);
    }

    public void saveReport(ChanPinHuoLiuQuXiang report) {
        ChanPinHuoLiuQuXiang lastMonth = getLastMonthData(report.getDanweiid(), report.getNianfen(), report.getYuefen(), report.getChanpinmingcheng());
        CumulativeUtils.calculateCumulative(report, lastMonth);
        if (report.getId() != null) {
            cphlqxMapper.updateById(report);
        } else {
            cphlqxMapper.insert(report);
        }
    }

    public ChanPinHuoLiuQuXiang getById(Long id) {
        return cphlqxMapper.selectById(id);
    }

    public boolean deleteReport(Long id) {
        ChanPinHuoLiuQuXiang report = cphlqxMapper.selectById(id);
        if (report == null) return false;
        String status = report.getZhuangtai();
        if (!"待上报".equals(status) && !"返回修改".equals(status) && status != null) return false;
        cphlqxMapper.deleteById(id);
        return true;
    }

    public boolean submitReport(Long id, Integer operatorId) {
        ChanPinHuoLiuQuXiang report = cphlqxMapper.selectById(id);
        if (report == null) return false;
        String status = report.getZhuangtai();
        if (!"待上报".equals(status) && !"返回修改".equals(status)) return false;
        int approvalLevel = calculateApprovalLevel(report.getDanweiid());
        String newStatus = "待审批" + approvalLevel;
        report.setZhuangtai(newStatus);
        cphlqxMapper.updateById(report);
        recordApprovalHistory(id.intValue(), operatorId, "上报数据", null, newStatus);
        return true;
    }

    public boolean existsRecord(Integer danweiId, Integer nianfen, Integer yuefen, String chanpinmingcheng, Long excludeId) {
        QueryWrapper<ChanPinHuoLiuQuXiang> wrapper = new QueryWrapper<>();
        wrapper.eq("DanWeiID", danweiId);
        wrapper.eq("NianFen", nianfen);
        wrapper.eq("YueFen", yuefen);
        wrapper.eq("ChanPinMingCheng", chanpinmingcheng);
        if (excludeId != null) wrapper.ne("ID", excludeId);
        return cphlqxMapper.selectCount(wrapper) > 0;
    }

    public int calculateApprovalLevel(Integer danweiId) {
        int depth = 0;
        Integer currentId = danweiId;
        while (currentId != null && currentId != YANKUANG_JITUAN_ID && currentId != 0) {
            Danwei danwei = danweiMapper.selectById(currentId);
            if (danwei == null) break;
            depth++;
            currentId = danwei.getShangjidanweiid();
        }
        return Math.max(1, depth);
    }

    public boolean canOperateRecord(Integer operatorDanweiId, Integer recordDanweiId) {
        if (operatorDanweiId.equals(recordDanweiId)) return true;
        Integer currentId = recordDanweiId;
        while (currentId != null) {
            Danwei danwei = danweiMapper.selectById(currentId);
            if (danwei == null) break;
            if (danwei.getShangjidanweiid() != null && danwei.getShangjidanweiid().equals(operatorDanweiId)) return true;
            currentId = danwei.getShangjidanweiid();
        }
        return false;
    }

    public String approveReport(Long id, Integer operatorDanweiId, boolean forceApprove, Integer operatorId) {
        ChanPinHuoLiuQuXiang report = cphlqxMapper.selectById(id);
        if (report == null) return "记录不存在";
        String currentStatus = report.getZhuangtai();
        if (currentStatus == null || !currentStatus.startsWith("待审批")) return "当前状态不可审批";
        Integer recordDanweiId = report.getDanweiid();
        if (!canOperateRecord(operatorDanweiId, recordDanweiId)) return "无权限审批此记录";
        
        Danwei recordDanwei = danweiMapper.selectById(recordDanweiId);
        if (recordDanwei == null) return "记录单位不存在";
        Integer directParentId = recordDanwei.getShangjidanweiid();
        
        if (!operatorDanweiId.equals(directParentId) && !forceApprove) {
            String pendingUnits = getPendingApprovalUnits(operatorDanweiId, recordDanweiId);
            if (pendingUnits != null && !pendingUnits.isEmpty()) return "SKIP_WARNING:" + pendingUnits;
        }
        
        try {
            int currentLevel = Integer.parseInt(currentStatus.replace("待审批", ""));
            String newStatus = (currentLevel <= 1) ? "审批通过" : "待审批" + (currentLevel - 1);
            report.setZhuangtai(newStatus);
            cphlqxMapper.updateById(report);
            
            Danwei operatorDanwei = danweiMapper.selectById(operatorDanweiId);
            String danweiName = operatorDanwei != null ? operatorDanwei.getMingcheng() : "";
            recordApprovalHistory(id.intValue(), operatorId, "审批通过", null, currentStatus + "(" + danweiName + ")");
            return "审批成功";
        } catch (NumberFormatException e) {
            return "状态格式错误";
        }
    }

    private String getPendingApprovalUnits(Integer operatorDanweiId, Integer recordDanweiId) {
        List<String> pendingUnits = new ArrayList<>();
        Integer currentId = recordDanweiId;
        while (currentId != null && !currentId.equals(operatorDanweiId)) {
            Danwei danwei = danweiMapper.selectById(currentId);
            if (danwei == null) break;
            Integer parentId = danwei.getShangjidanweiid();
            if (parentId != null && !parentId.equals(operatorDanweiId)) {
                Danwei parentDanwei = danweiMapper.selectById(parentId);
                if (parentDanwei != null) pendingUnits.add(parentDanwei.getMingcheng());
            }
            currentId = parentId;
        }
        return String.join("、", pendingUnits);
    }

    public String returnForModification(Long id, Integer operatorDanweiId, Integer operatorId) {
        ChanPinHuoLiuQuXiang report = cphlqxMapper.selectById(id);
        if (report == null) return "记录不存在";
        if (!canOperateRecord(operatorDanweiId, report.getDanweiid())) return "无权限退回此记录";
        String currentStatus = report.getZhuangtai();
        if (currentStatus == null || "待上报".equals(currentStatus)) return "当前状态不可退回";
        report.setZhuangtai("待上报");
        cphlqxMapper.updateById(report);
        Danwei operatorDanwei = danweiMapper.selectById(operatorDanweiId);
        String danweiName = operatorDanwei != null ? operatorDanwei.getMingcheng() : "";
        recordApprovalHistory(id.intValue(), operatorId, "返回修改", null, currentStatus + "(" + danweiName + ")");
        return "退回成功";
    }

    public boolean canEdit(Long id) {
        ChanPinHuoLiuQuXiang report = cphlqxMapper.selectById(id);
        if (report == null) return false;
        String status = report.getZhuangtai();
        return "待上报".equals(status) || "返回修改".equals(status);
    }

    public Map<String, Object> getApprovalStatus(Long id) {
        ChanPinHuoLiuQuXiang report = cphlqxMapper.selectById(id);
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

    private void recordApprovalHistory(Integer shenqingId, Integer operatorId, String jieguo, String yijian, String beizhu) {
        ChanPinQuXiangShenPiRecord record = new ChanPinQuXiangShenPiRecord();
        record.setJihuaid(shenqingId);
        record.setShenpirenid(operatorId != null ? operatorId : 0);
        record.setShenpishijian(new Date());
        record.setShenpijieguo(jieguo);
        record.setShenpiyijian(yijian);
        record.setBeizhu(beizhu);
        chanPinQuXiangShenPiRecordMapper.insert(record);
    }
}
