package com.itheima.qukuailian.config;

import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.config.exceptions.ConfigException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * FISCO BCOS 配置：创建 BcosSDK 与 Client Bean
 * <p>
 * 仅在 bcos.enabled=true 时生效（见 application.yml）。配置文件的加载方式：
 * <ul>
 *   <li>classpath:bcos/config.toml —— IDE 本地运行（Maven 构建后位于 classpath）</li>
 *   <li>绝对路径 /app/bcos/config.toml —— Docker 部署（见 Dockerfile）</li>
 * </ul>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "bcos", name = "enabled", havingValue = "true")
public class BcosConfig {

    @Value("${bcos.config-file}")
    private String configFile;

    /** 群组 id（FISCO BCOS 3.x 为整数，默认 group0 对应 0） */
    @Value("${bcos.group-id:0}")
    private int groupId;

    @Bean
    public BcosSDK bcosSDK() throws ConfigException {
        String path = resolveConfigPath();
        log.info("初始化 FISCO BCOS SDK，配置文件: {}", path);
        return BcosSDK.build(path);
    }

    @Bean
    public Client client(BcosSDK bcosSDK) {
        Client client = bcosSDK.getClient(groupId);
        log.info("FISCO BCOS Client 初始化完成, group={}, blockNumber={}", groupId, safeBlockNumber(client));
        return client;
    }

    private String resolveConfigPath() throws ConfigException {
        if (configFile.startsWith("classpath:")) {
            String location = configFile.substring("classpath:".length());
            try {
                return new ClassPathResource(location).getFile().getAbsolutePath();
            } catch (IOException e) {
                throw new ConfigException("无法解析 classpath 配置: " + configFile, e);
            }
        }
        return configFile;
    }

    private Object safeBlockNumber(Client client) {
        try {
            return client.getBlockNumber().getBlockNumber();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
