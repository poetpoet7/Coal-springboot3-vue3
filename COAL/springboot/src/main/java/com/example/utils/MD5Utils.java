package com.example.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 兼容C# UTF-16LE编码的MD5加密工具
 * C#系统使用Encoding.Unicode（即UTF-16LE）对密码进行编码后计算MD5
 * 密码格式：CE-0B-FD-15-05-9B-68-D6-76-88-88-4D-7A-3D-3E-8C（大写，连字符分隔）
 */
public class MD5Utils {

    /**
     * 生成与C#兼容的MD5哈希（UTF-16LE编码）
     * 
     * @param input 原始密码
     * @return MD5哈希值，格式：XX-XX-XX-...-XX（大写，连字符分隔）
     */
    public static String md5WithUtf16LE(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("输入字符串不能为空");
        }

        try {
            // 1. 将字符串转换为UTF-16LE字节数组（与C#的Encoding.Unicode一致）
            byte[] utf16Bytes = input.getBytes(StandardCharsets.UTF_16LE);

            // 2. 计算MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(utf16Bytes);

            // 3. 转换为大写十六进制，用连字符分隔
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hashBytes.length; i++) {
                if (i > 0) {
                    sb.append("-");
                }
                sb.append(String.format("%02X", hashBytes[i] & 0xff));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    /**
     * 验证密码（兼容C#系统）
     * 
     * @param rawPassword    用户输入的原始密码
     * @param hashedPassword 数据库中存储的哈希密码
     * @return 密码是否匹配
     */
    public static boolean verify(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }

        String computed = md5WithUtf16LE(rawPassword);
        return computed.equalsIgnoreCase(hashedPassword);
    }

    /**
     * 测试方法 - 验证加密结果
     */
    public static void main(String[] args) {
        // 测试几个常见密码
        String[] testPasswords = { "123456", "admin", "password" };

        System.out.println("MD5 UTF-16LE 加密测试：");
        System.out.println("=".repeat(60));
        for (String pwd : testPasswords) {
            String hash = md5WithUtf16LE(pwd);
            System.out.println("密码: " + pwd);
            System.out.println("MD5:  " + hash);
            System.out.println("-".repeat(60));
        }
    }
}
