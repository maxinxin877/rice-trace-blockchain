package com.itheima.qukuailian.service;

import com.itheima.qukuailian.entity.RiceChainProof;

import java.util.List;
import java.util.Map;

/**
 * 链上存证服务（接口文档 v1.1 上链规则）
 * <p>业务侧只提交摘要：dataHash / fileHashes / businessType / businessId；
 * 上链异步执行，业务表与 rice_chain_proof 记录 chainStatus（PENDING/SUCCESS/FAILED），失败可重试。</p>
 */
public interface ChainProofService {

    /**
     * 提交业务摘要上链（幂等：同一 businessType+businessId 复用存证记录）
     *
     * @param businessType 业务类型：FIELD/PLANTING_BATCH/FARMING_LOG/...
     * @param businessId   业务 ID
     * @param dataHash     业务摘要哈希（sha256: 前缀）
     * @param fileHashes   关联文件哈希（可空）
     */
    RiceChainProof submit(String businessType, String businessId, String dataHash, List<String> fileHashes);

    /**
     * 查询业务对象链上存证（接口文档 v1.1 §7.6）
     */
    RiceChainProof getProof(String businessType, String businessId);

    /**
     * 核验链上存证（接口文档 v1.1 §7.7）：
     * 重新计算当前摘要与链上摘要比对。
     * 阶段 7 接入真实 FISCO 后，currentHash 从 MySQL 业务数据重算、chainHash 从链上读取。
     */
    Map<String, Object> verify(String businessType, String businessId);
}
