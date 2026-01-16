package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.entity.Danwei;
import com.example.entity.TongjiCzcptouzikuaibao;
import com.example.mapper.DanweiMapper;
import com.example.mapper.TongjiCzcptouzikuaibaoMapper;
import com.example.utils.CumulativeUtils;
import jakarta.annotation.Resource;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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
     * 导出Excel - 格式与提供的Excel文件完全一致
     */
    public byte[] exportExcel(Integer danweiId, Integer nianfen, Integer yuefen, String leibie) throws IOException {
        List<Map<String, Object>> data = getReportData(danweiId, nianfen, yuefen, leibie);

        // 查询选中单位名称
        Danwei selectedUnit = danweiMapper.selectById(danweiId);
        String unitName = selectedUnit != null ? selectedUnit.getMingcheng() : "兖矿集团公司";

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("产能投资快报");

        // 创建样式
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 18);
        titleStyle.setFont(titleFont);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.RIGHT);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setAlignment(HorizontalAlignment.LEFT);
        textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        textStyle.setBorderTop(BorderStyle.THIN);
        textStyle.setBorderBottom(BorderStyle.THIN);
        textStyle.setBorderLeft(BorderStyle.THIN);
        textStyle.setBorderRight(BorderStyle.THIN);

        CellStyle totalStyle = workbook.createCellStyle();
        totalStyle.setAlignment(HorizontalAlignment.RIGHT);
        totalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        totalStyle.setBorderTop(BorderStyle.THIN);
        totalStyle.setBorderBottom(BorderStyle.THIN);
        totalStyle.setBorderLeft(BorderStyle.THIN);
        totalStyle.setBorderRight(BorderStyle.THIN);
        totalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        totalStyle.setFont(boldFont);

        int rowNum = 0;

        // 第1行：标题
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("产值、主要产品产量及固定资产投资快报");
        titleCell.setCellStyle(titleStyle);

        // 第2行：日期和编制单位
        Row infoRow = sheet.createRow(rowNum++);
        Cell unitCell = infoRow.createCell(0);
        unitCell.setCellValue("编制单位：" + unitName);
        Cell dateCell = infoRow.createCell(10);
        dateCell.setCellValue(nianfen + "年" + (yuefen != null ? yuefen : 1) + "月（" + leibie + "）");

        rowNum++; // 空行

        // 定义列头 - 与Excel文件完全一致
        // 总列数计算：2(序号+单位名称) + 3(经营总值) + 1(新产品) + 2(工业销售) + 34(主要产品) + 1(企业用电量) + 2(固定资产)
        // = 45
        int totalColumns = 45;

        // 第1行header
        Row headerRow1 = sheet.createRow(rowNum++);
        headerRow1.setHeightInPoints(30);

        // 第2行header
        Row headerRow2 = sheet.createRow(rowNum++);
        headerRow2.setHeightInPoints(25);

        int colIdx = 0;

        // 空列（序号）- 合并两行
        Cell cellXh1 = headerRow1.createCell(colIdx);
        cellXh1.setCellValue("");
        cellXh1.setCellStyle(headerStyle);
        Cell cellXh2 = headerRow2.createCell(colIdx);
        cellXh2.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 2, rowNum - 1, colIdx, colIdx));
        colIdx++;

        // 单位名称 - 合并两行
        Cell cellDw1 = headerRow1.createCell(colIdx);
        cellDw1.setCellValue("单位名称");
        cellDw1.setCellStyle(headerStyle);
        Cell cellDw2 = headerRow2.createCell(colIdx);
        cellDw2.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 2, rowNum - 1, colIdx, colIdx));
        colIdx++;

        // 经营总值(万元) - 合并3列
        Cell cellJyzz = headerRow1.createCell(colIdx);
        cellJyzz.setCellValue("经营总值(万元)");
        cellJyzz.setCellStyle(headerStyle);
        headerRow1.createCell(colIdx + 1).setCellStyle(headerStyle);
        headerRow1.createCell(colIdx + 2).setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 2, rowNum - 2, colIdx, colIdx + 2));
        // 子列
        headerRow2.createCell(colIdx).setCellValue("计");
        headerRow2.getCell(colIdx).setCellStyle(headerStyle);
        headerRow2.createCell(colIdx + 1).setCellValue("工业产值(现价)");
        headerRow2.getCell(colIdx + 1).setCellStyle(headerStyle);
        headerRow2.createCell(colIdx + 2).setCellValue("其他产值、营业额");
        headerRow2.getCell(colIdx + 2).setCellStyle(headerStyle);
        colIdx += 3;

        // 新产品值价(万元) - 合并两行
        Cell cellXcp = headerRow1.createCell(colIdx);
        cellXcp.setCellValue("新产品值价(万元)");
        cellXcp.setCellStyle(headerStyle);
        headerRow2.createCell(colIdx).setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 2, rowNum - 1, colIdx, colIdx));
        colIdx++;

        // 工业销售产值(万元) - 合并2列
        Cell cellGyxs = headerRow1.createCell(colIdx);
        cellGyxs.setCellValue("工业销售产值（万元）");
        cellGyxs.setCellStyle(headerStyle);
        headerRow1.createCell(colIdx + 1).setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 2, rowNum - 2, colIdx, colIdx + 1));
        headerRow2.createCell(colIdx).setCellValue("计");
        headerRow2.getCell(colIdx).setCellStyle(headerStyle);
        headerRow2.createCell(colIdx + 1).setCellValue("其中：出口交货值");
        headerRow2.getCell(colIdx + 1).setCellStyle(headerStyle);
        colIdx += 2;

        // 主要产品产量 - 34个产品
        String[] products = {
                "原煤(吨)", "精煤(吨)", "尿素(吨)", "甲醇(吨)", "醋酸(吨)", "焦炭(吨)",
                "醋酸乙酯(吨)", "醋酸丁酯(吨)", "醋酐(吨)", "碳酸二甲酯(吨)", "合成氨(吨)",
                "丁醇(吨)", "聚甲醛(吨)", "改质沥青(吨)", "蒽油(吨)", "复合肥(吨)",
                "铝锭(吨)", "碳素制品(吨)", "铝铸材(吨)", "铝挤压材(吨)", "发电量(万千瓦时)",
                "皮带运输机(台)", "输送带(M2)", "电缆(米)", "液压支架(架)", "刮板运输机(台)",
                "高岭土(吨)", "轻柴油(吨)", "重柴油(吨)", "石脑油(吨)", "液化石油气",
                "硫磺(吨)", "硫酸铵(吨)", "其他产品"
        };

        Cell cellZycpcl = headerRow1.createCell(colIdx);
        cellZycpcl.setCellValue("主要产品产量");
        cellZycpcl.setCellStyle(headerStyle);
        // 合并主要产品产量标题
        for (int i = 1; i < products.length; i++) {
            headerRow1.createCell(colIdx + i).setCellStyle(headerStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 2, rowNum - 2, colIdx, colIdx + products.length - 1));

        // 产品子列
        for (int i = 0; i < products.length; i++) {
            headerRow2.createCell(colIdx + i).setCellValue(products[i]);
            headerRow2.getCell(colIdx + i).setCellStyle(headerStyle);
        }
        colIdx += products.length;

        // 企业用电量(万千瓦时) - 合并两行
        Cell cellQyydl = headerRow1.createCell(colIdx);
        cellQyydl.setCellValue("企业用电量(万千瓦时)");
        cellQyydl.setCellStyle(headerStyle);
        headerRow2.createCell(colIdx).setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 2, rowNum - 1, colIdx, colIdx));
        colIdx++;

        // 固定资产投资(万元) - 合并2列
        Cell cellGdzctz = headerRow1.createCell(colIdx);
        cellGdzctz.setCellValue("固定资产投资（万元）");
        cellGdzctz.setCellStyle(headerStyle);
        headerRow1.createCell(colIdx + 1).setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 2, rowNum - 2, colIdx, colIdx + 1));
        headerRow2.createCell(colIdx).setCellValue("合计");
        headerRow2.getCell(colIdx).setCellStyle(headerStyle);
        headerRow2.createCell(colIdx + 1).setCellValue("技术改造");
        headerRow2.getCell(colIdx + 1).setCellStyle(headerStyle);

        // 合并第一行标题
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalColumns - 1));

        // 判断是否为累计模式
        boolean isLeiji = "累计".equals(leibie);

        // 填充数据
        for (Map<String, Object> row : data) {
            Row dataRow = sheet.createRow(rowNum++);
            TongjiCzcptouzikuaibao t = (TongjiCzcptouzikuaibao) row.get("data");
            boolean isTotal = row.get("isTotal") != null && (Boolean) row.get("isTotal");
            CellStyle currentStyle = isTotal ? totalStyle : dataStyle;
            CellStyle currentTextStyle = isTotal ? totalStyle : textStyle;

            colIdx = 0;

            // 序号
            Cell cellXuhao = dataRow.createCell(colIdx++);
            cellXuhao.setCellValue(row.get("xuhao").toString());
            cellXuhao.setCellStyle(currentTextStyle);

            // 单位名称
            Cell cellName = dataRow.createCell(colIdx++);
            cellName.setCellValue(row.get("danweiMingcheng").toString());
            cellName.setCellStyle(currentTextStyle);

            // 经营总值 - 计
            dataRow.createCell(colIdx).setCellValue(
                    getDecimalValue(isLeiji ? t.getJingyingzongzhihejileiji() : t.getJingyingzongzhiheji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            // 经营总值 - 工业产值(现价)
            dataRow.createCell(colIdx)
                    .setCellValue(getDecimalValue(isLeiji ? t.getGongyechanzhileiji() : t.getGongyechanzhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            // 经营总值 - 其他产值
            dataRow.createCell(colIdx)
                    .setCellValue(getDecimalValue(isLeiji ? t.getQitachanzhileiji() : t.getQitachanzhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 新产品值价
            dataRow.createCell(colIdx)
                    .setCellValue(getDecimalValue(isLeiji ? t.getXinchanpinjiazhileiji() : t.getXinchanpinjiazhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 工业销售产值 - 计
            dataRow.createCell(colIdx)
                    .setCellValue(getDecimalValue(isLeiji ? t.getGyxsczhejileiji() : t.getGyxsczheji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            // 工业销售产值 - 出口交货值
            dataRow.createCell(colIdx)
                    .setCellValue(getDecimalValue(isLeiji ? t.getChukoujiaohuozhileiji() : t.getChukoujiaohuozhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 主要产品产量 - 34个产品
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getYuanmeileiji() : t.getYuanmei()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getJingmeileiji() : t.getJingmei()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getNiaosuleiji() : t.getNiaosu()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getJiachunleiji() : t.getJiachun()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getCusuanleiji() : t.getCusuan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getJiaotanleiji() : t.getJiaotan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getCusuanyizhileiji() : t.getCusuanyizhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getCusuandingzhileiji() : t.getCusuandingzhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getCuganleiji() : t.getCugan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getTansuanerjiazhileiji() : t.getTansuanerjiazhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getHechenganleiji() : t.getHechengan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getDingchunleiji() : t.getDingchun()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getJujiaquanleiji() : t.getJujiaquan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getGaizhiliqingleiji() : t.getGaizhiliqing()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getEnyouleiji() : t.getEnyou()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getFuhefeileiji() : t.getFuhefei()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getLvdingleiji() : t.getLvding()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getTansuzhipinleiji() : t.getTansuzhipin()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getLvzhucaileiji() : t.getLvzhucai()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getLvjiyacaileiji() : t.getLvjiyacai()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getFadianliangleiji() : t.getFadianliang()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getPidaiyunshujileiji() : t.getPidaiyunshuji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getShusongdaileiji() : t.getShusongdai()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getDianlanleiji() : t.getDianlan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getYeyazhijialeiji() : t.getYeyazhijia()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getGuabanyunshujileiji() : t.getGuabanyunshuji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getGaolingtuleiji() : t.getGaolingtu()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getQingchaiiyouleiji() : t.getQingchaiyou()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getZhongchaiyouleiji() : t.getZhongchaiyou()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getShinaoyouleiji() : t.getShinaoyou()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getYehuashiyouqileiji() : t.getYehuashiyouqi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getLiuhuangleiji() : t.getLiuhuang()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(isLeiji ? t.getLiuhuanganleiji() : t.getLiuhuangan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getQitachanpinleiji() : t.getQitachanpin()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 企业用电量
            dataRow.createCell(colIdx)
                    .setCellValue(getLongValue(isLeiji ? t.getQiyeyongdianliangleiji() : t.getQiyeyongdianliang()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 固定资产投资 - 合计
            dataRow.createCell(colIdx)
                    .setCellValue(getDecimalValue(isLeiji ? t.getGdzctzhejileiji() : t.getGdzctzheji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            // 固定资产投资 - 技术改造
            dataRow.createCell(colIdx)
                    .setCellValue(getDecimalValue(isLeiji ? t.getJishugaizaoleiji() : t.getJishugaizao()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
        }

        // 设置列宽
        sheet.setColumnWidth(0, 8 * 256); // 序号
        sheet.setColumnWidth(1, 20 * 256); // 单位名称
        for (int i = 2; i < totalColumns; i++) {
            sheet.setColumnWidth(i, 12 * 256);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
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
     * BigDecimal转Double，处理null
     */
    private double getDecimalValue(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    /**
     * Long转double，处理null
     */
    private double getLongValue(Long value) {
        return value != null ? value.doubleValue() : 0.0;
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
