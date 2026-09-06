package com.itheima.qukuailian.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 摘要工具：SHA-256，统一输出带 "sha256:" 前缀（接口文档 v1.1 摘要格式）
 */
public final class HashUtils {

    private static final String PREFIX = "sha256:";

    private HashUtils() {
    }

    /** 字符串 SHA-256 */
    public static String sha256(String text) {
        return PREFIX + hex(sha256Bytes(text.getBytes(StandardCharsets.UTF_8)));
    }

    /** 字节数组 SHA-256 */
    public static String sha256(byte[] data) {
        return PREFIX + hex(sha256Bytes(data));
    }

    /** 输入流 SHA-256（流式读取，适合大文件） */
    public static String sha256(InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            return PREFIX + hex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /** 去除前缀的纯十六进制 */
    public static String rawHex(String prefixed) {
        return prefixed != null && prefixed.startsWith(PREFIX) ? prefixed.substring(PREFIX.length()) : prefixed;
    }

    private static byte[] sha256Bytes(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
