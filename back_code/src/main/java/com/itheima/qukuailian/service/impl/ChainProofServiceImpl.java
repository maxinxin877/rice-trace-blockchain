package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.qukuailian.bcos.EvidenceContractService;
import com.itheima.qukuailian.bcos.model.ChainTxResult;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.entity.RiceChainProof;
import com.itheima.qukuailian.mapper.RiceChainProofMapper;
import com.itheima.qukuailian.service.ChainProofService;
import com.itheima.qukuailian.utils.IdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 链上存证实现：
 * <ol>
 *   <li>落库 rice_chain_proof（状态 PENDING，业务唯一键 business_type+business_id）</li>
 *   <li>异步上链：
 *       bcos.enabled=true 时调用 FISCO BCOS Evidence 存证合约（真实交易）；
 *       未启用时模拟上链成功（便于开发演示）。成功回填 txId/blockHeight/chainTime 并置 SUCCESS，失败置 FAILED 可重试。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChainProofServiceImpl implements ChainProofService {

    private final RiceChainProofMapper chainProofMapper;

    /** 真实链服务（bcos.enabled=true 时存在） */
    @Autowired(required = false)
    private EvidenceContractService evidenceContractService;

    @Value("${bcos.enabled:false}")
    private boolean bcosEnabled;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chain-proof-submit");
        t.setDaemon(true);
        return t;
    });

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiceChainProof submit(String businessType, String businessId, String dataHash, List<String> fileHashes) {
        RiceChainProof proof = chainProofMapper.selectOne(new LambdaQueryWrapper<RiceChainProof>()
                .eq(RiceChainProof::getBusinessType, businessType)
                .eq(RiceChainProof::getBusinessId, businessId));
        if (proof == null) {
            proof = new RiceChainProof();
            proof.setChainProofId(IdGen.generate("CP"));
            proof.setBusinessType(businessType);
            proof.setBusinessId(businessId);
            proof.setDataHash(dataHash);
            proof.setFileHashes(fileHashes);
            proof.setChainStatus("PENDING");
            chainProofMapper.insert(proof);
        } else {
            proof.setDataHash(dataHash);
            proof.setFileHashes(fileHashes);
            proof.setChainStatus("PENDING");
            proof.setChainError("");
            chainProofMapper.updateById(proof);
        }
        asyncSubmit(proof);
        return proof;
    }

    @Override
    public RiceChainProof getProof(String businessType, String businessId) {
        RiceChainProof proof = chainProofMapper.selectOne(new LambdaQueryWrapper<RiceChainProof>()
                .eq(RiceChainProof::getBusinessType, businessType)
                .eq(RiceChainProof::getBusinessId, businessId));
        if (proof == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(),
                    "链上存证不存在: " + businessType + "/" + businessId);
        }
        return proof;
    }

    @Override
    public Map<String, Object> verify(String businessType, String businessId) {
        RiceChainProof proof = getProof(businessType, businessId);
        Map<String, Object> result = new HashMap<>();
        result.put("businessType", businessType);
        result.put("businessId", businessId);
        result.put("txId", proof.getTxId());
        result.put("blockHeight", proof.getBlockHeight());
        result.put("chainTime", proof.getChainTime());
        result.put("dataHash", proof.getDataHash());
        result.put("chainHash", proof.getDataHash());
        // 阶段7增强：真实链模式下从链上回读存证比对（Evidence.get）
        result.put("currentHash", proof.getDataHash());
        if (!"SUCCESS".equals(proof.getChainStatus())) {
            result.put("verified", false);
            result.put("message", "存证状态为 " + proof.getChainStatus() + "，请稍后重试或检查上链日志");
        } else {
            result.put("verified", true);
        }
        return result;
    }

    /** 异步上链：真实 FISCO 存证合约 或 模拟（开发演示） */
    private void asyncSubmit(RiceChainProof proof) {
        EXECUTOR.submit(() -> {
            try {
                if (bcosEnabled && evidenceContractService != null) {
                    // 真实链：调用 Evidence 存证合约
                    ChainTxResult txResult = evidenceContractService
                            .store(proof.getBusinessType(), proof.getBusinessId(), proof.getDataHash());
                    proof.setTxId(txResult.getTxHash());
                    proof.setBlockHeight(txResult.getBlockNumber());
                } else {
                    // 模拟链：回填模拟交易信息
                    Thread.sleep(200);
                    proof.setTxId("0xmock-" + UUID.randomUUID().toString().replace("-", "").substring(0, 32));
                    proof.setBlockHeight(1000L + ThreadLocalRandom.current().nextLong(900000));
                }
                proof.setChainTime(LocalDateTime.now());
                proof.setChainStatus("SUCCESS");
                proof.setChainError("");
                chainProofMapper.updateById(proof);
                log.info("[链上存证] 上链成功: business={}/{}, txId={}, blockHeight={}",
                        proof.getBusinessType(), proof.getBusinessId(), proof.getTxId(), proof.getBlockHeight());
            } catch (Exception e) {
                log.warn("[链上存证] 上链失败: business={}/{}", proof.getBusinessType(), proof.getBusinessId(), e);
                proof.setChainStatus("FAILED");
                proof.setChainError(e.getMessage());
                chainProofMapper.updateById(proof);
            }
        });
    }
}
