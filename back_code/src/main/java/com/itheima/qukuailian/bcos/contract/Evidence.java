package com.itheima.qukuailian.bcos.contract;

import org.fisco.bcos.sdk.abi.TypeReference;
import org.fisco.bcos.sdk.abi.datatypes.Bool;
import org.fisco.bcos.sdk.abi.datatypes.Function;
import org.fisco.bcos.sdk.abi.datatypes.Type;
import org.fisco.bcos.sdk.abi.datatypes.Utf8String;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.contract.Contract;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Evidence 存证合约的 Java 包装类（手写，风格与 SDK 2.9.x 生成代码一致）。
 * <p>
 * 注意：若使用控制台/CodeGen 重新生成了包装类，请以生成的版本为准替换本文件。
 * 部署合约请使用控制台（见 docs/FISCO_BCOS搭建指南.md），拿到地址后填入
 * application.yml 的 bcos.contract-address。
 */
public class Evidence extends Contract {

    /** 合约 ABI（与 contracts/Evidence.sol 对应） */
    public static final String ABI = "["
            + "{\"constant\":false,\"inputs\":[{\"name\":\"businessType\",\"type\":\"string\"},{\"name\":\"businessId\",\"type\":\"string\"},{\"name\":\"dataHash\",\"type\":\"string\"}],\"name\":\"store\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
            + "{\"constant\":true,\"inputs\":[{\"name\":\"businessType\",\"type\":\"string\"},{\"name\":\"businessId\",\"type\":\"string\"}],\"name\":\"get\",\"outputs\":[{\"name\":\"dataHash\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"uint256\"},{\"name\":\"operator\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},"
            + "{\"constant\":true,\"inputs\":[{\"name\":\"businessType\",\"type\":\"string\"},{\"name\":\"businessId\",\"type\":\"string\"}],\"name\":\"exists\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"}"
            + "]";

    /** 合约字节码（本工程用控制台部署，此处留空） */
    public static final String BINARY = "";

    protected Evidence(String contractAddress, Client client, CryptoKeyPair credential) {
        // Contract 构造器参数顺序：(binary, contractAddress, client, credential)
        super(BINARY, contractAddress, client, credential);
    }

    /**
     * 按合约地址加载
     */
    public static Evidence load(String contractAddress, Client client, CryptoKeyPair credential) {
        return new Evidence(contractAddress, client, credential);
    }

    /**
     * 存证：businessType + businessId -> dataHash
     */
    public TransactionReceipt store(String businessType, String businessId, String dataHash)
            throws ContractException {
        Function function = new Function("store",
                Arrays.asList(new Utf8String(businessType), new Utf8String(businessId), new Utf8String(dataHash)),
                Collections.emptyList());
        return executeTransaction(function);
    }

    /**
     * 查询存证，返回 [dataHash, timestamp, operator]
     */
    public List<String> get(String businessType, String businessId) throws ContractException {
        Function function = new Function("get",
                Arrays.asList(new Utf8String(businessType), new Utf8String(businessId)),
                Arrays.asList(new TypeReference<Utf8String>() {}, new TypeReference<org.fisco.bcos.sdk.abi.datatypes.generated.Uint256>() {},
                        new TypeReference<org.fisco.bcos.sdk.abi.datatypes.Address>() {}));
        List<Type> results = executeCallWithMultipleValueReturn(function);
        return Arrays.asList(
                (String) results.get(0).getValue(),
                results.get(1).getValue().toString(),
                (String) results.get(2).getValue());
    }

    /**
     * 是否存在存证
     */
    public Boolean exists(String businessType, String businessId) throws ContractException {
        Function function = new Function("exists",
                Arrays.asList(new Utf8String(businessType), new Utf8String(businessId)),
                Collections.singletonList(new TypeReference<Bool>() {}));
        List<Type> results = executeCallWithMultipleValueReturn(function);
        return (Boolean) results.get(0).getValue();
    }
}
