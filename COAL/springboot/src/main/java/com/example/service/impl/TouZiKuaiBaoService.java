package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.entity.Danwei;
import com.example.entity.TongjiCzcptouzikuaibao;
import com.example.mapper.DanweiMapper;
import com.example.mapper.TongjiCzcptouzikuaibaoMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
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
        // 获取所有单位
        List<Danwei> allUnits = danweiMapper.selectList(null);

        // 根据类别预加载数据
        // 对于"累计"模式，需要查询每个单位在该年份中 <= 指定月份的最新一条记录
        Map<Integer, TongjiCzcptouzikuaibao> dataCache = new HashMap<>();
        if ("累计".equals(leibie)) {
            dataCache = loadCumulativeData(nianfen, yuefen);
        }

        // 构建单位树
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
     * 加载累计模式的数据
     * 对于每个单位，查询该年份中 <= 指定月份的最新一条记录
     * 
     * @param nianfen 年份
     * @param yuefen  月份
     * @return 单位ID -> 数据记录的映射
     */
    private Map<Integer, TongjiCzcptouzikuaibao> loadCumulativeData(Integer nianfen, Integer yuefen) {
        Map<Integer, TongjiCzcptouzikuaibao> result = new HashMap<>();

        // 查询该年份中 <= 指定月份的所有记录
        QueryWrapper<TongjiCzcptouzikuaibao> wrapper = new QueryWrapper<>();
        wrapper.eq("NianFen", nianfen);
        wrapper.le("YueFen", yuefen);
        wrapper.orderByDesc("YueFen"); // 按月份降序，这样同一单位的最新记录在前面

        List<TongjiCzcptouzikuaibao> allData = tongjiMapper.selectList(wrapper);

        // 对于每个单位，只保留最新月份的记录（第一条遇到的）
        for (TongjiCzcptouzikuaibao data : allData) {
            Integer danweiId = data.getDanweiid();
            if (!result.containsKey(danweiId)) {
                result.put(danweiId, data);
            }
        }

        return result;
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

        // 查询当前单位的实际数据
        TongjiCzcptouzikuaibao actualData = null;

        if ("累计".equals(leibie)) {
            // 累计模式：从缓存中获取（该年份中 <= 指定月份的最新记录）
            actualData = dataCache.get(currentUnit.getId().intValue());
        } else {
            // 本月模式：查询指定月份的记录
            QueryWrapper<TongjiCzcptouzikuaibao> wrapper = new QueryWrapper<>();
            wrapper.eq("DanWeiID", currentUnit.getId());
            wrapper.eq("NianFen", nianfen);
            if (yuefen != null) {
                wrapper.eq("YueFen", yuefen);
            }
            List<TongjiCzcptouzikuaibao> dataList = tongjiMapper.selectList(wrapper);
            actualData = dataList.isEmpty() ? null : dataList.get(0);
        }

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
     * 累加子节点数据
     */
    private TongjiCzcptouzikuaibao aggregateChildrenData(List<DanweiNode> children) {
        TongjiCzcptouzikuaibao aggregated = new TongjiCzcptouzikuaibao();

        for (DanweiNode child : children) {
            if (child.getData() != null) {
                addData(aggregated, child.getData());
            }
        }

        return aggregated;
    }

    /**
     * 累加两个数据对象的所有数值字段
     */
    private void addData(TongjiCzcptouzikuaibao target, TongjiCzcptouzikuaibao source) {
        try {
            Field[] fields = TongjiCzcptouzikuaibao.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object sourceValue = field.get(source);

                if (sourceValue instanceof BigDecimal) {
                    BigDecimal targetValue = (BigDecimal) field.get(target);
                    BigDecimal sum = (targetValue != null ? targetValue : BigDecimal.ZERO)
                            .add((BigDecimal) sourceValue);
                    field.set(target, sum);
                } else if (sourceValue instanceof Long) {
                    Long targetValue = (Long) field.get(target);
                    Long sum = (targetValue != null ? targetValue : 0L) + (Long) sourceValue;
                    field.set(target, sum);
                } else if (sourceValue instanceof Integer &&
                        !field.getName().equals("danweiid") &&
                        !field.getName().equals("nianfen") &&
                        !field.getName().equals("yuefen")) {
                    Integer targetValue = (Integer) field.get(target);
                    Integer sum = (targetValue != null ? targetValue : 0) + (Integer) sourceValue;
                    field.set(target, sum);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
     * 计算累计值：本月累计 = 上月累计 + 本月值
     */
    private void calculateCumulativeValues(TongjiCzcptouzikuaibao current, TongjiCzcptouzikuaibao lastMonth) {
        // 经营总值累计
        current.setJingyingzongzhihejileiji(addBigDecimal(
                current.getJingyingzongzhiheji(),
                lastMonth != null ? lastMonth.getJingyingzongzhihejileiji() : null));

        // 工业产值累计
        current.setGongyechanzhileiji(addBigDecimal(
                current.getGongyechanzhi(),
                lastMonth != null ? lastMonth.getGongyechanzhileiji() : null));

        // 其他产值累计
        current.setQitachanzhileiji(addBigDecimal(
                current.getQitachanzhi(),
                lastMonth != null ? lastMonth.getQitachanzhileiji() : null));

        // 新产品价值累计
        current.setXinchanpinjiazhileiji(addBigDecimal(
                current.getXinchanpinjiazhi(),
                lastMonth != null ? lastMonth.getXinchanpinjiazhileiji() : null));

        // 工业销售产值累计
        current.setGyxsczhejileiji(addBigDecimal(
                current.getGyxsczheji(),
                lastMonth != null ? lastMonth.getGyxsczhejileiji() : null));

        // 出口交货值累计
        current.setChukoujiaohuozhileiji(addBigDecimal(
                current.getChukoujiaohuozhi(),
                lastMonth != null ? lastMonth.getChukoujiaohuozhileiji() : null));

        // 企业用电量累计
        current.setQiyeyongdianliangleiji(addLong(
                current.getQiyeyongdianliang(),
                lastMonth != null ? lastMonth.getQiyeyongdianliangleiji() : null));

        // 固定资产投资累计
        current.setGdzctzhejileiji(addBigDecimal(
                current.getGdzctzheji(),
                lastMonth != null ? lastMonth.getGdzctzhejileiji() : null));

        // 技术改造累计
        current.setJishugaizaoleiji(addBigDecimal(
                current.getJishugaizao(),
                lastMonth != null ? lastMonth.getJishugaizaoleiji() : null));

        // 主要产品产量累计
        current.setYuanmeileiji(addLong(current.getYuanmei(), lastMonth != null ? lastMonth.getYuanmeileiji() : null));
        current.setJingmeileiji(addLong(current.getJingmei(), lastMonth != null ? lastMonth.getJingmeileiji() : null));
        current.setNiaosuleiji(addLong(current.getNiaosu(), lastMonth != null ? lastMonth.getNiaosuleiji() : null));
        current.setJiachunleiji(addLong(current.getJiachun(), lastMonth != null ? lastMonth.getJiachunleiji() : null));
        current.setCusuanleiji(addLong(current.getCusuan(), lastMonth != null ? lastMonth.getCusuanleiji() : null));
        current.setJiaotanleiji(addLong(current.getJiaotan(), lastMonth != null ? lastMonth.getJiaotanleiji() : null));
        current.setCusuanyizhileiji(
                addLong(current.getCusuanyizhi(), lastMonth != null ? lastMonth.getCusuanyizhileiji() : null));
        current.setCusuandingzhileiji(
                addLong(current.getCusuandingzhi(), lastMonth != null ? lastMonth.getCusuandingzhileiji() : null));
        current.setCuganleiji(addLong(current.getCugan(), lastMonth != null ? lastMonth.getCuganleiji() : null));
        current.setTansuanerjiazhileiji(
                addLong(current.getTansuanerjiazhi(), lastMonth != null ? lastMonth.getTansuanerjiazhileiji() : null));
        current.setHechenganleiji(
                addLong(current.getHechengan(), lastMonth != null ? lastMonth.getHechenganleiji() : null));
        current.setDingchunleiji(
                addLong(current.getDingchun(), lastMonth != null ? lastMonth.getDingchunleiji() : null));
        current.setJujiaquanleiji(
                addLong(current.getJujiaquan(), lastMonth != null ? lastMonth.getJujiaquanleiji() : null));
        current.setGaizhiliqingleiji(
                addLong(current.getGaizhiliqing(), lastMonth != null ? lastMonth.getGaizhiliqingleiji() : null));
        current.setEnyouleiji(addLong(current.getEnyou(), lastMonth != null ? lastMonth.getEnyouleiji() : null));
        current.setFuhefeileiji(addLong(current.getFuhefei(), lastMonth != null ? lastMonth.getFuhefeileiji() : null));
        current.setLvdingleiji(addLong(current.getLvding(), lastMonth != null ? lastMonth.getLvdingleiji() : null));
        current.setTansuzhipinleiji(
                addLong(current.getTansuzhipin(), lastMonth != null ? lastMonth.getTansuzhipinleiji() : null));
        current.setLvzhucaileiji(
                addLong(current.getLvzhucai(), lastMonth != null ? lastMonth.getLvzhucaileiji() : null));
        current.setLvjiyacaileiji(
                addLong(current.getLvjiyacai(), lastMonth != null ? lastMonth.getLvjiyacaileiji() : null));
        current.setFadianliangleiji(
                addLong(current.getFadianliang(), lastMonth != null ? lastMonth.getFadianliangleiji() : null));
        current.setPidaiyunshujileiji(
                addLong(current.getPidaiyunshuji(), lastMonth != null ? lastMonth.getPidaiyunshujileiji() : null));
        current.setShusongdaileiji(
                addLong(current.getShusongdai(), lastMonth != null ? lastMonth.getShusongdaileiji() : null));
        current.setDianlanleiji(addLong(current.getDianlan(), lastMonth != null ? lastMonth.getDianlanleiji() : null));
        current.setYeyazhijialeiji(
                addLong(current.getYeyazhijia(), lastMonth != null ? lastMonth.getYeyazhijialeiji() : null));
        current.setGuabanyunshujileiji(
                addLong(current.getGuabanyunshuji(), lastMonth != null ? lastMonth.getGuabanyunshujileiji() : null));
        current.setGaolingtuleiji(
                addLong(current.getGaolingtu(), lastMonth != null ? lastMonth.getGaolingtuleiji() : null));
        current.setQingchaiiyouleiji(
                addLong(current.getQingchaiyou(), lastMonth != null ? lastMonth.getQingchaiiyouleiji() : null));
        current.setZhongchaiyouleiji(
                addLong(current.getZhongchaiyou(), lastMonth != null ? lastMonth.getZhongchaiyouleiji() : null));
        current.setShinaoyouleiji(
                addLong(current.getShinaoyou(), lastMonth != null ? lastMonth.getShinaoyouleiji() : null));
        current.setYehuashiyouqileiji(
                addLong(current.getYehuashiyouqi(), lastMonth != null ? lastMonth.getYehuashiyouqileiji() : null));
        current.setLiuhuangleiji(
                addLong(current.getLiuhuang(), lastMonth != null ? lastMonth.getLiuhuangleiji() : null));
        current.setLiuhuanganleiji(
                addLong(current.getLiuhuangan(), lastMonth != null ? lastMonth.getLiuhuanganleiji() : null));
        current.setQitachanpinleiji(
                addLong(current.getQitachanpin(), lastMonth != null ? lastMonth.getQitachanpinleiji() : null));
    }

    /**
     * BigDecimal加法，处理null
     */
    private BigDecimal addBigDecimal(BigDecimal current, BigDecimal lastCumulative) {
        BigDecimal c = current != null ? current : BigDecimal.ZERO;
        BigDecimal l = lastCumulative != null ? lastCumulative : BigDecimal.ZERO;
        return c.add(l);
    }

    /**
     * Long加法，处理null
     */
    private Long addLong(Long current, Long lastCumulative) {
        long c = current != null ? current : 0L;
        long l = lastCumulative != null ? lastCumulative : 0L;
        return c + l;
    }

    /**
     * 根据ID获取记录
     */
    public TongjiCzcptouzikuaibao getById(Long id) {
        return tongjiMapper.selectById(id);
    }

    /**
     * 删除记录（仅允许删除待上报的）
     */
    public boolean deleteReport(Long id) {
        TongjiCzcptouzikuaibao report = tongjiMapper.selectById(id);
        if (report == null) {
            return false;
        }
        // 检查状态，只有待上报的才能删除
        if ("已上报".equals(report.getZhuangtai())) {
            return false;
        }
        tongjiMapper.deleteById(id);
        return true;
    }

    /**
     * 上报记录
     */
    public boolean submitReport(Long id) {
        TongjiCzcptouzikuaibao report = tongjiMapper.selectById(id);
        if (report == null) {
            return false;
        }
        report.setZhuangtai("已上报");
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
}
