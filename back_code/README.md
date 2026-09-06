# 基于区块链的水稻产品质量安全溯源系统（后端）

水稻产品从**确地块、确种子、确投入品、质量检验到一物一码**的全流程质量安全溯源。
关键状态变更、质检摘要、文件指纹以**摘要形式写入 FISCO BCOS 区块链**（防篡改存证），
监管端可进行产量平衡校验、风险预警、审计留痕与链上核验。接口遵循
[`接口文档 v1.1`](新建%20文本文档.txt)。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 框架 | Spring Boot 4.x + Spring MVC（RESTful JSON） |
| 认证 | JWT（`Authorization: Bearer`）+ 权限码注解 `@RequirePermission` |
| 数据访问 | MySQL 8 + MyBatis-Plus（3.5.17，分页、JSON 字段自动映射） |
| 缓存 | Redis（预留：扫码溯源结果缓存） |
| 区块链 | FISCO BCOS 3.x + Java SDK 2.9.3（**通用存证合约 Evidence**，链上仅存摘要） |
| 部署 | Docker Compose（MySQL/Redis/应用/Nginx 反向代理） |

> 未搭链时 `bcos.enabled=false`，上链动作**模拟成功**，业务全流程照常跑通；
> 接入真实链只需搭链、部署 `Evidence` 合约、改一行配置（见 `docs/FISCO_BCOS搭建指南.md`）。

## 项目结构

```
qukuailian/
├── src/main/java/com/itheima/qukuailian/
│   ├── config/          # JWT拦截器/权限、Redis、MyBatis-Plus、FISCO SDK、请求追踪
│   ├── common/          # 统一响应(Result)、错误码、全局异常、LoginUser、@RequirePermission
│   ├── utils/           # JWT工具、SHA-256、业务ID、权限常量、用户上下文
│   ├── controller/      # 认证/文件/地块/种植批次/农事/环境/入库/质检/加工/成品/防伪/监管/链上核验
│   ├── service/         # 业务层（含 AuditLogService、ChainProofService、RiceRegulationService）
│   ├── mapper/          # MyBatis-Plus Mapper
│   ├── entity/          # 17 张业务表实体
│   ├── dto/ vo/         # 请求/响应对象
│   └── bcos/            # Evidence 存证合约包装类 + 真实链服务
├── src/main/resources/
│   ├── contracts/       # Evidence.sol 存证合约
│   └── bcos/            # SDK 配置模板 config.toml
├── sql/init.sql         # 建库建表 + 角色种子用户
├── Dockerfile / docker-compose.yml / nginx/
└── docs/                # FISCO 搭建指南、部署指南
```

## 快速开始（开发环境，模拟链）

1. 初始化数据库（建库建表 + 种子账号）：按 [`docs/数据库初始化指南.md`](docs/数据库初始化指南.md) 执行 `sql/init.sql`，并把 `application-dev.yml` 的数据库密码改成你本机的（本机为 `1234`）。
2. IDEA 运行 `QukuailianApplication`。
3. 登录拿 JWT（种子账号如 `admin / 123456`，角色 RICE_ADMIN 拥有全部权限码）：

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# => {"code":0,"message":"success","data":{"token":"eyJ...","expiresIn":43200,"user":{...}},...}
```

4. 带 `Authorization: Bearer {token}` 调用业务接口（`code=0` 成功，错误码见文档 §2.4）。
5. 全流程冒烟测试（一键跑通：登录→地块→批次→入库→质检→加工→防伪码→监管校验→链上核验→看板）：

```powershell
pwsh scripts/smoke-test.ps1
```

### 接口总览（详见接口文档 §4）

| 模块 | 路径 | 说明 |
| --- | --- | --- |
| 认证 | `/auth/login`、`/auth/register`、`/auth/me` | 登录返回 Bearer JWT |
| 文件 | `/files` | 上传（SHA-256 去重）/元数据/下载 |
| 地块 | `/rice/fields` | CRUD + GIS 坐标哈希上链 |
| 种植批次 | `/rice/planting-batches` | 确地块确种子、状态流转、审计 |
| 农事记录 | `/rice/planting-batches/{id}/farming-logs` | 记录摘要上链（农药类强校验） |
| 环境数据 | `/rice/planting-batches/{id}/environment-records` | IOT/DRONE/MANUAL |
| 收储入库 | `/rice/storage-receipts` | 入库单摘要上链、批次置 STORED |
| 入库质检 | `/rice/storage-receipts/{id}/quality-tests` | 报告哈希 + 质检摘要上链 |
| 碾米加工 | `/rice/milling-batches` | 原粮-成品映射上链、产出率预警 |
| 成品批次 | `/rice/product-batches` | productBatchId 业务唯一 |
| 品牌防伪 | `/rice/trace-codes` | 批量生成/激活（上链）、窜货预警查询 |
| 监管 | `/rice/regulation/**` | 产量校验、风险预警处理、审计日志 |
| 链上核验 | `/rice/chain-proofs/{type}/{id}` | 存证查询/核验 |
| 看板 | `/rice/dashboard/summary` | 总览统计 |

## 接入真实 FISCO BCOS

按 [`docs/FISCO_BCOS搭建指南.md`](docs/FISCO_BCOS搭建指南.md)：
搭链 → 控制台部署 `Evidence` 存证合约 → 证书复制到 `bcos/` → 配置 `bcos.enabled=true`、回填 `bcos.contract-address`。
上链成功后 `rice_chain_proof` 回填真实 `txId/blockHeight/chainTime`，状态 SUCCESS，失败可重试。

## 部署

按 [`docs/部署指南.md`](docs/部署指南.md)：`docker compose up -d --build`，前端产物放 `frontend/dist`。

## 说明

- 链上**仅存摘要**（`dataHash/fileHashes/businessType/businessId/txId/blockHeight/chainTime`），
  原始业务明细以 MySQL 和文件系统为准（文档 §1.3）。
- 所有关键写操作自动记审计日志（`rice_audit_log`），监管端可查（文档 §7.5）。
- 上链为**异步**执行：业务先行落库，链上失败不阻断主流程，状态可查可重试（文档 §11.4）。
- `bcos/contract/Evidence.java` 为手写的合约包装类（与 SDK 2.9.x 生成代码同风格），
  SDK 升级后可用 `scripts/gen-contract.sh` 重新生成。
- 小程序端接口（`/mini/**`）按项目分工不在本仓库实现，后端已预留扫码日志/防伪码数据结构。
