# FISCO BCOS 搭建与后端接入指南

本指南带你完成：搭一条 FISCO BCOS 3.x 链 → 部署溯源合约 → 后端接入真实链。

> 版本选型：FISCO BCOS **v3.11.0**（当前最新稳定版）、Java SDK **fisco-bcos-java-sdk 2.9.3**。
> 后端已内置 **内存模拟链**（`bcos.mock-enabled=true`），不搭链也能先跑通业务；本指南用于切换真实链。

---

## 0. 环境准备

| 组件 | 要求 | 说明 |
| --- | --- | --- |
| 操作系统 | Linux 或 Windows + WSL2 | 链节点在 Windows 上直接跑比较麻烦，建议 WSL2 或云服务器 |
| Docker | 可选 | 方式二用 Docker 跑节点 |
| JDK | 8+（后端为 17） | 控制台与 SDK 需要 |

检查网络能否访问 GitHub（下载 build_chain.sh / 控制台）。网络受限时可尝试代理或 gitee 镜像。

---

## 1. 方式一：build_chain.sh 本地搭链（推荐）

```bash
cd ~ && mkdir -p fisco && cd fisco

# 1. 下载官方搭链脚本
curl -#LO https://github.com/FISCO-BCOS/FISCO-BCOS/releases/download/v3.11.0/build_chain.sh
chmod u+x build_chain.sh

# 2. 生成 4 节点单群组配置（-l 本机4节点，-p 端口）
#    30300=p2p端口, 20200=channel端口(SDK连这个), 8545=rpc端口
bash build_chain.sh -l 127.0.0.1:4 -p 30300,20200,8545

# 3. 启动
bash nodes/127.0.0.1/start_all.sh

# 4. 验证：查看节点进程
ps -ef | grep fisco-bcos | grep -v grep
# 日志无报错：tail -f nodes/127.0.0.1/node0/log/error.log
```

停止链：`bash nodes/127.0.0.1/stop_all.sh`

> 提示：`build_chain.sh` 可加 `-v 3.11.0` 显式指定版本；加 `-s` 生成国密版本（需与 SDK `useSMCrypto=true` 配套，默认不用国密）。

---

## 2. 方式二：Docker 跑节点（部署演示用）

```bash
# 先生成单节点配置
cd ~/fisco
bash build_chain.sh -l 127.0.0.1:1 -p 30300,20200,8545 -o docker-nodes

# 用官方镜像运行（镜像入口即 fisco-bcos 二进制）
docker run -d --name fisco-bcos \
  -v $PWD/docker-nodes/127.0.0.1/node0:/data \
  -p 20200:20200 -p 30300:30300 -p 8545:8545 \
  fiscoorg/fiscobcos:v3.11.0 -c /data/config.ini

docker logs -f fisco-bcos
```

> 若镜像启动参数有出入，以官方文档为准：<https://fisco-bcos-doc.readthedocs.io/zh-cn/latest/>

---

## 3. 下载控制台并部署合约

```bash
cd ~/fisco

# 1. 下载控制台（版本与链保持一致）
curl -#LO https://github.com/FISCO-BCOS/console/releases/download/v3.11.0/console.tar.gz
tar -xzf console.tar.gz && cd console

# 2. 拷贝节点 SDK 证书到控制台
cp -r ../nodes/127.0.0.1/sdk/* conf/

# 3. 放入存证合约（本仓库已提供：通用存证合约，链上只存业务摘要）
cp <项目路径>/src/main/resources/contracts/Evidence.sol contracts/solidity/

# 4. 启动控制台（交互式）
bash start.sh
```

在控制台内依次执行：

```
# 部署合约
deploy Evidence

# 输出示例：
# transaction hash: 0x...
# contract address: 0x2c5b1f0f0c1a5f8e6b7c...
# currentAccount: 0x...

# 查看合约地址
getAddress Evidence
```

记下 `contract address`，下一步回填到后端配置。

> 也可退出控制台后用非交互方式：`bash start.sh -deploy RiceTrace`。

---

## 4. 后端接入真实链

### 4.1 复制证书与 SDK 配置到项目根目录

```bash
cd <项目路径>   # 即 qukuailian 目录

# 证书目录
mkdir -p bcos/conf bcos/account
cp -r ~/fisco/nodes/127.0.0.1/sdk/* bcos/conf/

# SDK 配置模板（已放好相对路径，见模板内注释）
cp src/main/resources/bcos/config.toml bcos/
```

此时项目根目录结构：

```
qukuailian/
├── bcos/
│   ├── config.toml          # SDK 配置（路径相对项目根目录）
│   ├── conf/                # ca.crt / sdk.crt / sdk.key
│   └── account/             # 账户密钥（SDK 首次启动自动生成 ecdsa.pem）
├── src/...
```

> `.gitignore` 已忽略 `bcos/`，证书与私钥不会提交到仓库。

### 4.2 修改 application.yml

```yaml
bcos:
  enabled: true          # 开启真实链
  mock-enabled: false    # 关闭内存模拟链
  group-id: 0            # 默认群组 group0 对应整数 0
  config-file: bcos/config.toml
  contract-address: "0x2c5b1f0f0c1a5f8e6b7c..."   # 上一步记录的合约地址
```

### 4.3 验证

1. 启动后端（IDE 直接运行 `QukuailianApplication`，工作目录需为项目根目录）。
2. 登录拿 token：`POST /api/auth/login`（admin / 123456）。
3. 创建并上链一个批次：
   - `POST /api/product` 新建产品
   - `POST /api/batch` 创建批次（带 `token` 请求头）
   - `POST /api/batch/{id}/onchain` → 返回真实 `txHash`
4. 追加溯源记录：`POST /api/batch/{id}/trace`
5. 查链上数据：
   - `GET /api/trace/chain/{batchNo}`（纯链上）
   - `GET /api/trace/{batchNo}`（库 + 链）
   - `GET /api/block/latest`、`GET /api/block/number/{n}`、`GET /api/block/tx/{hash}`（区块浏览器）

---

## 5. 常见问题

| 现象 | 原因/解决 |
| --- | --- |
| `BcosSDK build failed` / 连接不上 | 节点未启动、20200 端口不通；`peers` 地址是否写对（容器内跑节点需写宿主机 IP） |
| 证书校验失败 | `bcos/conf` 下的证书必须来自**当前链**的 `nodes/127.0.0.1/sdk/` 目录 |
| `contract-address 未配置` | 忘记回填 4.2 的合约地址（Evidence 存证合约） |
| 交易失败 status 非 0 | 合约方法 require 未通过，如批次已存在/不存在；查看控制台交易回执 |
| Windows 下 build_chain.sh 无法执行 | 用 WSL2 或 Git Bash；或直接用方式二 Docker |
| 想重新生成合约 Java 包装类 | 见 `scripts/gen-contract.sh`（在 Linux/WSL 执行，生成 Evidence 包装类） |
| 想自己换合约 | 改 `src/main/resources/contracts/RiceTrace.sol`，重新部署并用代码生成器替换 `bcos/contract/RiceTrace.java` |

---

## 参考链接

- FISCO BCOS 3.x 文档：<https://fisco-bcos-doc.readthedocs.io/zh-cn/latest/>
- Java SDK 配置：<https://fisco-bcos-doc.readthedocs.io/zh-cn/latest/docs/sdk/java_sdk/configuration.html>
- GitHub Releases：<https://github.com/FISCO-BCOS/FISCO-BCOS/releases>
