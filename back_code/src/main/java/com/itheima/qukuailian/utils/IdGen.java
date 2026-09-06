package com.itheima.qukuailian.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务 ID 生成器：前缀 + 日期 + 4 位随机数，如 FIELD202607180001
 */
public final class IdGen {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private IdGen() {
    }

    public static String generate(String prefix) {
        String date = LocalDateTime.now().format(FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return prefix + date + random;
    }
}
