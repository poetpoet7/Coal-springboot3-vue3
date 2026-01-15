package com.example.utils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 累计值计算工具类
 * 通过反射自动处理所有 "xxx" 字段与对应的 "xxxleiji" 字段
 */
public class CumulativeUtils {

    // 缓存字段映射关系，避免每次都反射
    private static Map<String, FieldPair> fieldPairCache = null;

    /**
     * 字段对：本月字段 + 累计字段
     */
    private static class FieldPair {
        Field currentField; // 本月字段 (如 yuanmei)
        Field cumulativeField; // 累计字段 (如 yuanmeileiji)

        FieldPair(Field current, Field cumulative) {
            this.currentField = current;
            this.cumulativeField = cumulative;
            this.currentField.setAccessible(true);
            this.cumulativeField.setAccessible(true);
        }
    }

    /**
     * 自动计算所有累计字段
     * 规则：xxxleiji = 当前xxx值 + 上月xxxleiji值
     *
     * @param current   当前月数据（会被修改，设置累计值）
     * @param lastMonth 上月数据（可为null）
     */
    public static void calculateCumulative(Object current, Object lastMonth) {
        if (current == null) {
            return;
        }

        // 初始化字段映射缓存
        if (fieldPairCache == null) {
            initFieldPairCache(current.getClass());
        }

        try {
            for (FieldPair pair : fieldPairCache.values()) {
                Object currentValue = pair.currentField.get(current);
                Object lastCumulativeValue = lastMonth != null ? pair.cumulativeField.get(lastMonth) : null;

                // 根据字段类型进行累加
                if (pair.currentField.getType() == BigDecimal.class) {
                    BigDecimal c = currentValue != null ? (BigDecimal) currentValue : BigDecimal.ZERO;
                    BigDecimal l = lastCumulativeValue != null ? (BigDecimal) lastCumulativeValue : BigDecimal.ZERO;
                    pair.cumulativeField.set(current, c.add(l));
                } else if (pair.currentField.getType() == Long.class) {
                    Long c = currentValue != null ? (Long) currentValue : 0L;
                    Long l = lastCumulativeValue != null ? (Long) lastCumulativeValue : 0L;
                    pair.cumulativeField.set(current, c + l);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to calculate cumulative values", e);
        }
    }

    /**
     * 累加两个数据对象的所有数值字段（用于聚合子单位数据）
     *
     * @param target 目标对象（累加结果存入此对象）
     * @param source 源对象
     */
    public static void addNumericFields(Object target, Object source) {
        if (target == null || source == null) {
            return;
        }

        try {
            Field[] fields = target.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();

                // 跳过非数值字段和特定字段
                if (shouldSkipField(fieldName)) {
                    continue;
                }

                Object sourceValue = field.get(source);
                if (sourceValue == null) {
                    continue;
                }

                if (field.getType() == BigDecimal.class) {
                    BigDecimal targetValue = (BigDecimal) field.get(target);
                    BigDecimal sum = (targetValue != null ? targetValue : BigDecimal.ZERO)
                            .add((BigDecimal) sourceValue);
                    field.set(target, sum);
                } else if (field.getType() == Long.class) {
                    Long targetValue = (Long) field.get(target);
                    Long sum = (targetValue != null ? targetValue : 0L) + (Long) sourceValue;
                    field.set(target, sum);
                } else if (field.getType() == Integer.class && !isIdOrDateField(fieldName)) {
                    Integer targetValue = (Integer) field.get(target);
                    Integer sum = (targetValue != null ? targetValue : 0) + (Integer) sourceValue;
                    field.set(target, sum);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to add numeric fields", e);
        }
    }

    /**
     * 初始化字段对缓存
     * 查找所有 xxx 和 xxxleiji 配对的字段
     */
    private static synchronized void initFieldPairCache(Class<?> clazz) {
        if (fieldPairCache != null) {
            return;
        }

        fieldPairCache = new HashMap<>();
        Field[] fields = clazz.getDeclaredFields();

        // 构建字段名到字段的映射
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : fields) {
            fieldMap.put(field.getName().toLowerCase(), field);
        }

        // 查找所有 xxxleiji 字段，并匹配对应的 xxx 字段
        for (Field field : fields) {
            String name = field.getName().toLowerCase();
            if (name.endsWith("leiji")) {
                String baseName = name.substring(0, name.length() - 5); // 去掉 "leiji"
                Field currentField = fieldMap.get(baseName);
                if (currentField != null) {
                    // 确保类型一致
                    if (currentField.getType() == field.getType()) {
                        fieldPairCache.put(baseName, new FieldPair(currentField, field));
                    }
                }
            }
        }
    }

    /**
     * 判断是否应该跳过该字段
     */
    private static boolean shouldSkipField(String fieldName) {
        String lower = fieldName.toLowerCase();
        return lower.equals("id") || lower.equals("serialversionuid");
    }

    /**
     * 判断是否为ID或日期相关字段（不参与累加）
     */
    private static boolean isIdOrDateField(String fieldName) {
        String lower = fieldName.toLowerCase();
        return lower.equals("danweiid") || lower.equals("nianfen") || lower.equals("yuefen");
    }
}
