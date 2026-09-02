// SPDX-License-Identifier: MIT
pragma solidity ^0.6.10;

/**
 * 通用存证合约（接口文档 v1.1 上链规则）
 *
 * 链上仅保存业务摘要：businessType + businessId -> dataHash（+fileHashes 摘要见业务侧）。
 * 原始业务明细以 MySQL 和文件系统为准，本合约用于防篡改存证与核验。
 *
 * 部署：使用 FISCO BCOS 控制台，将本文件放入 console/contracts/solidity/ 后执行 deploy Evidence，
 *      成功后 getAddress Evidence 获取合约地址，回填 application.yml 的 bcos.contract-address。
 */
contract Evidence {

    struct Proof {
        string dataHash;    // 业务摘要哈希（sha256: 前缀）
        uint256 timestamp;  // 存证时间（Unix 秒）
        address operator;   // 存证提交方
    }

    // businessType -> businessId -> 存证
    mapping(string => mapping(string => Proof)) private proofs;

    event ProofStored(string businessType, string businessId, string dataHash,
                      address operator, uint256 timestamp);

    /**
     * 存证：同一 businessType+businessId 重复提交时覆盖更新
     */
    function store(string memory businessType, string memory businessId, string memory dataHash) public {
        require(bytes(businessType).length > 0, "businessType is empty");
        require(bytes(businessId).length > 0, "businessId is empty");
        require(bytes(dataHash).length > 0, "dataHash is empty");
        proofs[businessType][businessId] = Proof(dataHash, now, msg.sender);
        emit ProofStored(businessType, businessId, dataHash, msg.sender, now);
    }

    /**
     * 查询存证：返回 (dataHash, timestamp, operator)
     */
    function get(string memory businessType, string memory businessId) public view
        returns (string memory dataHash, uint256 timestamp, address operator) {
        Proof memory p = proofs[businessType][businessId];
        return (p.dataHash, p.timestamp, p.operator);
    }

    /**
     * 是否存在存证
     */
    function exists(string memory businessType, string memory businessId) public view returns (bool) {
        return bytes(proofs[businessType][businessId].dataHash).length > 0;
    }
}
