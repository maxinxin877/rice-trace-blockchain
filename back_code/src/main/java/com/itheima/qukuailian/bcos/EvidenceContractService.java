package com.itheima.qukuailian.bcos;

import com.itheima.qukuailian.bcos.contract.Evidence;
import com.itheima.qukuailian.bcos.model.ChainTxResult;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * FISCO BCOS 真实存证服务（基于 Evidence 通用存证合约）。
 * <p>生效条件：bcos.enabled=true。需要先按 docs/FISCO_BCOS搭建指南.md 部署合约并回填地址。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "bcos", name = "enabled", havingValue = "true")
public class EvidenceContractService {

    private final Evidence contract;

    public EvidenceContractService(Client client, @Value("${bcos.contract-address:}") String contractAddress) {
        if (!StringUtils.hasText(contractAddress)) {
            throw new IllegalStateException("bcos.contract-address 未配置：请先用控制台部署 Evidence 合约并回填地址");
        }
        // 使用 SDK 加载账户（首次运行会自动在配置的 keyStoreDir 生成密钥文件）
        CryptoKeyPair credential = client.getCryptoSuite().getCryptoKeyPair();
        this.contract = Evidence.load(contractAddress, client, credential);
        log.info("Evidence 存证合约加载成功, address={}, 账户={}", contractAddress, credential.getAddress());
    }

    /**
     * 业务摘要上链：businessType + businessId -> dataHash
     */
    public ChainTxResult store(String businessType, String businessId, String dataHash) {
        try {
            TransactionReceipt receipt = contract.store(businessType, businessId, dataHash);
            if (receipt == null || !receipt.isStatusOK()) {
                throw new IllegalStateException("存证交易失败, status="
                        + (receipt == null ? "null" : receipt.getStatus()));
            }
            log.info("存证上链成功: business={}/{}, txHash={}",
                    businessType, businessId, receipt.getTransactionHash());
            return new ChainTxResult(receipt.getTransactionHash(), parseBlockNumber(receipt.getBlockNumber()));
        } catch (Exception e) {
            log.error("存证上链失败: business={}/{}", businessType, businessId, e);
            throw new IllegalStateException("存证上链失败: " + e.getMessage(), e);
        }
    }

    /** SDK 回执的 blockNumber 是字符串（可能是十进制或 0x 十六进制），统一转 long */
    private long parseBlockNumber(String blockNumber) {
        if (blockNumber == null || blockNumber.isBlank()) {
            return 0L;
        }
        try {
            String value = blockNumber.trim();
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseLong(value.substring(2), 16);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
