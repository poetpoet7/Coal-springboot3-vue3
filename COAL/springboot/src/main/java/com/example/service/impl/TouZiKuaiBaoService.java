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
     * 导出Excel
     */
    public byte[] exportExcel(Integer danweiId, Integer nianfen, Integer yuefen, String leibie) throws IOException {
        List<Map<String, Object>> data = getReportData(danweiId, nianfen, yuefen, leibie);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("产能投资快报");

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "序号", "单位名称",
                "经营总值合计", "累计",
                "工业产值", "累计",
                "其他产值", "累计",
                "新产品价值", "累计",
                "工业销售产值合计", "累计",
                "出口交货值", "累计"
        };

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        for (Map<String, Object> row : data) {
            Row dataRow = sheet.createRow(rowNum++);
            TongjiCzcptouzikuaibao tongji = (TongjiCzcptouzikuaibao) row.get("data");

            dataRow.createCell(0).setCellValue(row.get("xuhao").toString());
            dataRow.createCell(1).setCellValue(row.get("danweiMingcheng").toString());
            dataRow.createCell(2).setCellValue(getDecimalValue(tongji.getJingyingzongzhiheji()));
            dataRow.createCell(3).setCellValue(getDecimalValue(tongji.getJingyingzongzhihejileiji()));
            dataRow.createCell(4).setCellValue(getDecimalValue(tongji.getGongyechanzhi()));
            dataRow.createCell(5).setCellValue(getDecimalValue(tongji.getGongyechanzhileiji()));
            dataRow.createCell(6).setCellValue(getDecimalValue(tongji.getQitachanzhi()));
            dataRow.createCell(7).setCellValue(getDecimalValue(tongji.getQitachanzhileiji()));
            dataRow.createCell(8).setCellValue(getDecimalValue(tongji.getXinchanpinjiazhi()));
            dataRow.createCell(9).setCellValue(getDecimalValue(tongji.getXinchanpinjiazhileiji()));
            dataRow.createCell(10).setCellValue(getDecimalValue(tongji.getGyxsczheji()));
            dataRow.createCell(11).setCellValue(getDecimalValue(tongji.getGyxsczhejileiji()));
            dataRow.createCell(12).setCellValue(getDecimalValue(tongji.getChukoujiaohuozhi()));
            dataRow.createCell(13).setCellValue(getDecimalValue(tongji.getChukoujiaohuozhileiji()));
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
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
}
