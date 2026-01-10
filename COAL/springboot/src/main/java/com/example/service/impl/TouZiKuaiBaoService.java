package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.entity.Danwei;
import com.example.entity.TongjiCzcptouzikuaibao;
import com.example.mapper.DanweiMapper;
import com.example.mapper.TongjiCzcptouzikuaibaoMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
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
     * @param yuefen   月份（可选）
     * @param leibie   类别（本月/本年）
     * @return 报表数据列表
     */
    public List<Map<String, Object>> getReportData(Integer danweiId, Integer nianfen, Integer yuefen, String leibie) {
        // 获取所有单位
        List<Danwei> allUnits = danweiMapper.selectList(null);

        // 构建单位树
        DanweiNode rootNode = buildUnitTreeWithData(danweiId, allUnits, nianfen, yuefen, leibie, 0, "");

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
     * 递归构建单位树并加载/累加数据
     */
    private DanweiNode buildUnitTreeWithData(Integer parentId, List<Danwei> allUnits,
            Integer nianfen, Integer yuefen, String leibie,
            int level, String parentXuhao) {
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
                    childXuhao);

            if (childNode != null) {
                node.getChildren().add(childNode);
                childIndex++;
            }
        }

        // 查询当前单位的实际数据
        QueryWrapper<TongjiCzcptouzikuaibao> wrapper = new QueryWrapper<>();
        wrapper.eq("DanWeiID", currentUnit.getId());
        wrapper.eq("NianFen", nianfen);
        if ("本月".equals(leibie) && yuefen != null) {
            wrapper.eq("YueFen", yuefen);
        }
        List<TongjiCzcptouzikuaibao> dataList = tongjiMapper.selectList(wrapper);

        // 判断是否有实际数据或子节点
        boolean hasActualData = !dataList.isEmpty();
        boolean hasChildren = !node.getChildren().isEmpty();

        if (hasActualData) {
            // 有实际数据，直接使用
            node.setData(dataList.get(0));
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
     * 导出Excel - 与前端表格完全一致
     */
    public byte[] exportExcel(Integer danweiId, Integer nianfen, Integer yuefen, String leibie) throws IOException {
        List<Map<String, Object>> data = getReportData(danweiId, nianfen, yuefen, leibie);

        // 查询选中单位名称
        Danwei selectedUnit = danweiMapper.selectById(danweiId);
        String unitName = selectedUnit != null ? selectedUnit.getMingcheng() : "";

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

        // 第2行：日期
        Row dateRow = sheet.createRow(rowNum++);
        Cell dateCell = dateRow.createCell(0);
        if ("本月".equals(leibie) && yuefen != null) {
            dateCell.setCellValue(nianfen + "年" + yuefen + "月（本月）");
        } else {
            dateCell.setCellValue(nianfen + "年");
        }

        // 第3行：编制单位
        Row unitRow = sheet.createRow(rowNum++);
        Cell unitCell = unitRow.createCell(0);
        unitCell.setCellValue("编制单位：" + unitName);

        rowNum++; // 空行

        // 定义列头 - 与前端完全一致
        // 基础列（不带计/累计）
        String[] basicHeaders = { "序号", "单位名称", "经营总值(万元)", "工业产值(现价)(万元)" };

        // 带 计/累计 的列
        String[] pairedHeaders = {
                "其他产值(万元)", "新产品产值(万元)", "工业销售产值(万元)", "出口交货值(万元)",
                "90年不变价", "企业用电量(度)", "固定资产投资(万元)", "技术改造(万元)",
                "原煤(吨)", "精煤(吨)", "尿素(吨)", "甲醇(吨)", "醋酸(吨)", "焦炭(吨)",
                "醋酸乙酯(吨)", "醋酸丁酯(吨)", "醋酐(吨)", "碳酸二甲酯(吨)", "合成氨(吨)",
                "丁醇(吨)", "聚甲醛(吨)", "改质沥青(吨)", "蒽油(吨)", "复合肥(吨)",
                "铝锭(吨)", "碳素制品(吨)", "铝铸材(吨)", "铝挤压材(吨)", "发电量(度)",
                "皮带运输机(米)", "输送带(米)", "电缆(米)", "液压支架(架)", "刮板运输机(台)",
                "高岭土(吨)", "轻柴油(吨)", "重柴油(吨)", "石脑油(吨)", "液化石油气(吨)",
                "其他产品(吨)", "硫磺(吨)", "硫酸铵(吨)", "去年原煤(吨)", "去年精煤(吨)"
        };

        // 计算总列数：4基础列 + 44双列(每个2列)
        int totalColumns = basicHeaders.length + pairedHeaders.length * 2;

        // 第1行header：主标题行（需合并单元格）
        Row headerRow1 = sheet.createRow(rowNum++);
        headerRow1.setHeightInPoints(25);

        // 第2行header：计/累计 子标题行
        Row headerRow2 = sheet.createRow(rowNum++);
        headerRow2.setHeightInPoints(20);

        int colIdx = 0;

        // 基础列（合并两行）
        for (String header : basicHeaders) {
            Cell cell1 = headerRow1.createCell(colIdx);
            cell1.setCellValue(header);
            cell1.setCellStyle(headerStyle);

            Cell cell2 = headerRow2.createCell(colIdx);
            cell2.setCellStyle(headerStyle);

            // 合并单元格
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                    rowNum - 2, rowNum - 1, colIdx, colIdx));
            colIdx++;
        }

        // 带 计/累计 的双列
        for (String header : pairedHeaders) {
            // 主标题（合并两列）
            Cell cell1 = headerRow1.createCell(colIdx);
            cell1.setCellValue(header);
            cell1.setCellStyle(headerStyle);

            Cell cell1b = headerRow1.createCell(colIdx + 1);
            cell1b.setCellStyle(headerStyle);

            // 合并主标题
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                    rowNum - 2, rowNum - 2, colIdx, colIdx + 1));

            // 子标题：计 和 累计
            Cell cellJi = headerRow2.createCell(colIdx);
            cellJi.setCellValue("计");
            cellJi.setCellStyle(headerStyle);

            Cell cellLeiji = headerRow2.createCell(colIdx + 1);
            cellLeiji.setCellValue("累计");
            cellLeiji.setCellStyle(headerStyle);

            colIdx += 2;
        }

        // 合并第一行标题
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, totalColumns - 1));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, totalColumns - 1));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, totalColumns - 1));

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

            // 经营总值(万元) - 只有计
            Cell cellJyzz = dataRow.createCell(colIdx++);
            cellJyzz.setCellValue(getDecimalValue(t.getJingyingzongzhiheji()));
            cellJyzz.setCellStyle(currentStyle);

            // 工业产值(现价) - 只有计
            Cell cellGycz = dataRow.createCell(colIdx++);
            cellGycz.setCellValue(getDecimalValue(t.getGongyechanzhi()));
            cellGycz.setCellStyle(currentStyle);

            // 其他产值
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getQitachanzhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getQitachanzhileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 新产品产值
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getXinchanpinjiazhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getXinchanpinjiazhileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 工业销售产值
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getGyxsczheji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getGyxsczhejileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 出口交货值
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getChukoujiaohuozhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getChukoujiaohuozhileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 90年不变价
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getJiushinianbubianjia()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getJiushinianbubianjialeiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 企业用电量
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getQiyeyongdianliang()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getQiyeyongdianliangleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 固定资产投资
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getGdzctzheji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getGdzctzhejileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 技术改造
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getJishugaizao()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getDecimalValue(t.getJishugaizaoleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 原煤
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getYuanmei()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getYuanmeileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 精煤
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getJingmei()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getJingmeileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 尿素
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getNiaosu()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getNiaosuleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 甲醇
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getJiachun()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getJiachunleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 醋酸
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getCusuan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getCusuanleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 焦炭
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getJiaotan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getJiaotanleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 醋酸乙酯
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getCusuanyizhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getCusuanyizhileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 醋酸丁酯
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getCusuandingzhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getCusuandingzhileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 醋酐
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getCugan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getCuganleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 碳酸二甲酯
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getTansuanerjiazhi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getTansuanerjiazhileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 合成氨
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getHechengan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getHechenganleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 丁醇
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getDingchun()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getDingchunleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 聚甲醛
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getJujiaquan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getJujiaquanleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 改质沥青
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getGaizhiliqing()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getGaizhiliqingleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 蒽油
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getEnyou()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getEnyouleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 复合肥
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getFuhefei()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getFuhefeileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 铝锭
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getLvding()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getLvdingleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 碳素制品
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getTansuzhipin()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getTansuzhipinleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 铝铸材
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getLvzhucai()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getLvzhucaileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 铝挤压材
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getLvjiyacai()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getLvjiyacaileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 发电量
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getFadianliang()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getFadianliangleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 皮带运输机
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getPidaiyunshuji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getPidaiyunshujileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 输送带
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getShusongdai()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getShusongdaileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 电缆
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getDianlan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getDianlanleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 液压支架
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getYeyazhijia()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getYeyazhijialeiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 刮板运输机
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getGuabanyunshuji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getGuabanyunshujileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 高岭土
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getGaolingtu()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getGaolingtuleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 轻柴油
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getQingchaiyou()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getQingchaiiyouleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 重柴油
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getZhongchaiyou()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getZhongchaiyouleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 石脑油
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getShinaoyou()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getShinaoyouleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 液化石油气
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getYehuashiyouqi()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getYehuashiyouqileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 其他产品
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getQitachanpin()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getQitachanpinleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 硫磺
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getLiuhuang()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getLiuhuangleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 硫酸铵
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getLiuhuangan()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getLiuhuanganleiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 去年原煤
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getQunianyuanmei()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getQunianyuanmeileiji()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);

            // 去年精煤
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getQunianjingmei()));
            dataRow.getCell(colIdx++).setCellStyle(currentStyle);
            dataRow.createCell(colIdx).setCellValue(getLongValue(t.getQunianjingmeileiji()));
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
}
