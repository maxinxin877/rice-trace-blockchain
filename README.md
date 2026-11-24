# 水稻产品质量安全溯源系统

基于区块链技术的水稻全链路溯源平台，实现从种植到餐桌的完整追溯。

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 前端 | Vue 3 + TypeScript + Vite | 8.x |
| UI | Element Plus + Pinia + Vue Router | - |
| 图表 | ECharts + Leaflet | - |
| 后端 | Spring Boot + MyBatis-Plus + MySQL | 4.x / 3.5+ / 8.0 |
| 安全 | JWT (HS512) + Lombok | - |
| 区块链 | FISCO BCOS（可选启用） | - |

## 项目结构

```
rice-trace-blockchain/
├── front_code/                 # 前端 Vue3
│   ├── src/
│   │   ├── api/modules/        # API 模块 (auth/traceability/regulation/dashboard/productBatch)
│   │   ├── api/mock/data/      # Mock 数据
│   │   ├── components/         # 通用组件 (EChart/MapViewer/StatusTag/MobileShell)
│   │   ├── composables/        # 组合式函数
│   │   ├── layouts/            # 布局组件
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── views/              # 页面
│   │   │   ├── login/          # 登录
│   │   │   ├── mini/           # 小程序扫码端 (4 页)
│   │   │   └── rice/           # 管理端
│   │   │       ├── planting-batches/   # 种植批次
│   │   │       ├── farming-logs/       # 农事记录
│   │   │       ├── environments/        # 环境监测
│   │   │       ├── milling-batches/    # 加工批次
│   │   │       ├── storage-receipts/   # 入库单
│   │   │       ├── product-batches/    # 成品批次
│   │   │       ├── trace-codes/         # 防伪码管理
│   │   │       ├── trace/              # 窜货预警 / 追溯中心
│   │   │       ├── collect/            # 扫码采集
│   │   │       ├── package/            # 包装编码（箱码/托盘码）
│   │   │       ├── quality/            # 不合格品处置
│   │   │       ├── recall/             # 召回管理
│   │   │       ├── analytics/          # 质量分析
│   │   │       ├── regulation/         # 监管审计
│   │   │       └── dashboard/           # 数据看板
│   │   └── styles/             # 全局样式
│   ├── .env.development        # Mock 模式配置
│   ├── .env.local              # 本地联调配置 (不提交)
│   ├── vite.config.ts          # Vite + Proxy
│   └── package.json
│
├── back_code/                  # 后端 Spring Boot
│   ├── src/main/java/com/itheima/qukuailian/
│   │   ├── controller/         # 21 个 REST Controller
│   │   ├── service/impl/       # 21 个业务 Service
│   │   ├── mapper/             # 24 个 MyBatis Mapper
│   │   ├── entity/             # 24 个 数据库实体
│   │   ├── dto/ vo/            # 请求/响应 DTO
│   │   ├── enums/              # 状态枚举
│   │   ├── config/             # WebMvc/Redis/MyBatisPlus 配置
│   │   ├── security/           # JWT 工具类
│   │   └── QuukuailianApplication.java
│   ├── src/main/resources/
│   │   ├── mapper/*.xml        # MyBatis XML
│   │   └── application*.yml    # 配置文件 (dev/test/prod)
│   ├── sql/init.sql            # 数据库初始化脚本 (18 表 + 6 种子账号)
│   ├── sql/trace_recall_module.sql    # 增补：追溯查询 / 召回 / 包装单元
│   ├── sql/collect_quality_module.sql # 增补：扫码采集 / 不合格品处置
│   ├── scripts/                # 冒烟测试 / 权限一致性 / 数据复位脚本
│   └── pom.xml
│
├── docs/
│   ├── 部署指南.md
│   ├── 数据库初始化指南.md
│   └── FISCO_BCOS搭建指南.md
│
└── .gitignore                  # 三层 gitignore (根 + 前端 + 后端)
```

## 功能模块

### 管理端（6 角色 × 权限菜单）

| 模块 | 功能 | 角色 |
|---|---|---|
| 基础数据 | 地块管理、种植批次、农事记录、环境监测 | 管理员/种植户 |
| 生产加工 | 入库单、入库质检、加工批次、成品批次 | 仓储员/加工员 |
| 扫码采集 | 扫码枪/PDA 统一采集、幂等防重、离线补传、补录审核 | 全业务角色 |
| 包装编码 | 单品码 → 箱码 → 托盘码 层级绑定与展开 | 加工员/品牌运营 |
| 防伪溯源 | 防伪码生成/激活、扫码记录、窜货预警、追溯查询 | 品牌运营 |
| 质量管理 | 不合格品处置（隔离/返工/报废/让步接收）、质量分析 | 仓储员/加工员/品牌 |
| 召回管理 | 影响范围计算 → 审批 → 冻结 → 逐条处置 → 关闭归档 | 品牌运营/监管人员 |
| 生产防错 | 冻结/召回/隔离/休药期/质检五类规则校验 | 全部业务角色 |
| 监管审计 | 产量平衡校验、风险预警处理、审计日志、链上存证 | 监管人员 |
| 数据看板 | 统计卡片 + ECharts 图表 | 全部角色 |

### 小程序扫码端

| 页面 | 功能 |
|---|---|
| 扫码溯源 | 产品详情 + 全链路溯源时间轴 |
| 真伪鉴别 | 正品/未激活/窜货/假冒 四级判定 |
| 认证证书 | 绿色食品/有机/质检报告 |
| 链上核验 | 区块高度、交易哈希、链上摘要比对 |

### 核心业务流程

```
地块 → 种植批次 → 农事记录/环境监测 → 入库质检 → 加工 → 包装 → 成品 → 防伪码 → 扫码溯源
 🌾      🌱          📝 🌡️            📦 🔬     ⚙️    📦     🍚     🔐      📱
```

## 追溯与召回

- **追溯查询**（`/rice/trace-center`）：输入单品码/批次号/入库单号自动识别类型，还原
  「地块 → 种植 → 收储 → 加工 → 包装 → 一物一码 → 扫码流通」全链路，
  输出链路图、时间轴、链路关系、链上存证与影响范围。
- **召回管理**（`/rice/recalls`）：影响范围自动计算 → 审批 → 发布即冻结（成品批次 `LOCKED`、
  防伪码 `DISABLED`）→ 逐条登记下架/回收/隔离/销毁 → 关闭归档，召回指令与关闭结论上链。
- **生产防错**（`/rice/compliance/check`）：批次冻结、召回冻结、不合格品隔离、休药期未满、质检不合格五类规则，
  已接入入库、碾米加工、赋码激活三个环节。
- **扫码采集**（`/rice/scan-collect`）：扫码枪/PDA/移动端统一入口，自动识别码类型并带出业务数据，
  支持幂等防重、离线队列补传、人工补录审核。
- **不合格品处置**（`/rice/quality/disposals`）：评审通过即隔离，执行返工/报废/让步接收后按结论解冻，关闭归档。
- **包装编码**（`/rice/packages`）：箱码绑定单品码、托盘码绑定箱码，扫码后任意层级都可展开到单品并追溯上游。
- **质量分析**（`/rice/analytics`）：合格率趋势、异常分布、召回绩效、可解释风险评分。

## 快速开始

### 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | 21+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Node.js | 20+ |
| npm | 10+ |

### 1. 初始化数据库

```powershell
# 启动 MySQL 服务
Start-Service MySQL80

# 执行初始化脚本 (创建库 + 18 表 + 种子账号)
Get-Content -Raw "back_code/sql/init.sql" | mysql -uroot -p123456
```

### 2. 启动后端（终端 1）

```powershell
cd back_code

# 首次运行：Maven 下载依赖
.\mvnw.cmd spring-boot:run

# 启动成功：Tomcat started on port 8080 (http) with context path '/api/v1'
```

### 3. 启动前端（终端 2）

```powershell
cd front_code

# 安装依赖
npm install

# 启动开发服务器 (.env.local 已配置 VITE_USE_MOCK=false 对接真实后端)
npm run dev

# 启动成功：➜ Local: http://localhost:3000/
```

### 4. 访问系统

浏览器打开 **http://localhost:3000**

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 系统管理员（全部权限） |
| `farmer1` | `123456` | 种植户 |
| `keeper1` | `123456` | 仓储员 |
| `factory1` | `123456` | 加工员 |
| `brand1` | `123456` | 品牌运营 |
| `regulator1` | `123456` | 监管人员 |

### 服务架构

```
浏览器 http://localhost:3000
    ↓ axios 请求 /api/v1/*
Vite dev server (port 3000)
    ↓ proxy 代理 /api → http://localhost:8080
Spring Boot (port 8080, context-path /api/v1)
    ↓ MyBatis-Plus
MySQL (port 3306, 库名 qukuailian)
```

### Mock 模式（仅前端）

如果只看前端演示，不需要启动后端：

```powershell
cd front_code
# 删除 .env.local 或设置 VITE_USE_MOCK=true
npm run dev
```

前端会读取 `src/api/mock/data/` 下的 mock 数据。

## API 接口一览

后端 21 个 Controller 提供 RESTful API，统一前缀 `/api/v1`：

| 路径前缀 | Controller | 功能 |
|---|---|---|
| `/auth` | AuthController | 登录认证、用户信息 |
| `/files` | FileController | 文件上传下载 |
| `/rice/fields` | RiceFieldController | 地块 CRUD |
| `/rice/planting-batches` | RicePlantingBatchController | 种植批次 |
| `/rice/farming-logs` | RiceFarmingLogController | 农事记录 |
| `/rice/environment-records` | RiceEnvironmentRecordController | 环境监测 |
| `/rice/milling-batches` | RiceMillingBatchController | 加工批次 |
| `/rice/storage-receipts` | RiceStorageReceiptController | 入库单 |
| `/rice/product-batches` | RiceProductBatchController | 成品批次 |
| `/rice/trace-codes` | RiceTraceCodeController | 防伪码 |
| `/rice/channel-warnings` | RiceChannelWarningController | 窜货预警 |
| `/rice/collect` | RiceCollectController | 扫码采集、补录审核 |
| `/rice/package-units` | RicePackageUnitController | 箱码/托盘码 |
| `/rice/disposals` | RiceNonconformingDisposalController | 不合格品处置 |
| `/rice/recalls` | RiceRecallController | 召回任务与处置 |
| `/rice/trace-query` | RiceTraceQueryController | 正反向追溯查询 |
| `/rice/compliance` | RiceComplianceController | 生产防错校验 |
| `/rice/analytics` | RiceAnalyticsController | 质量分析看板 |
| `/mini/trace` | RiceMiniTraceController | 小程序扫码溯源 |
| `/rice/regulation/*` | RiceRegulationController | 监管审计、看板 |
| `/rice/chain-proofs` | RiceChainProofController | 链上存证 |

## 构建部署

### 前端生产构建

```powershell
cd front_code
npm run build
# 产物: front_code/dist/
```

### 后端生产构建

```powershell
cd back_code
.\mvnw.cmd clean package -DskipTests
# 产物: back_code/target/qukuailian-1.0.0.jar
java -jar target/qukuailian-1.0.0.jar
```

### Nginx 反向代理示例

```nginx
server {
    listen 80;
    server_name rice.example.com;

    # 前端静态资源
    location / {
        root /var/www/front_code/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/v1/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 配置说明

### 后端配置 (back_code/src/main/resources/application-dev.yml)

| 配置项 | 默认值 | 说明 |
|---|---|---|
| spring.datasource | root/123456@localhost:3306/qukuailian | MySQL 连接 |
| server.port | 8080 | 服务端口 |
| server.servlet.context-path | /api/v1 | API 前缀 |
| bcos.enabled | false | FISCO BCOS 开关 |

### 前端配置 (front_code/.env.local)

| 配置项 | 默认值 | 说明 |
|---|---|---|
| VITE_USE_MOCK | false | 是否使用 Mock 数据 |
| VITE_API_BASE_URL | /api/v1 | API 基础路径 |
| VITE_PROXY_TARGET | http://localhost:8080 | 开发代理的后端地址 |

## 开发约定

1. 前端管理端页面路由以 `/rice` 为模块前缀。
2. 小程序页面占位放在 `front_code/src/views/mini`，后续可迁移到 Uni-app 或微信小程序工程。
3. 字段、校验规则、状态流转以接口文档、开发文档与《需求对照与改进说明.md》为准。
4. 新增权限码必须同时改 `PermissionConstants`、`stores/user.ts`、菜单与路由，
   并执行 `back_code/scripts/check-permission-parity.ps1` 校验。

## 验证与冒烟测试

```bash
# 后端（显式指定端口，避免环境变量 SERVER_PORT 覆盖配置）
cd back_code && mvn -DskipTests spring-boot:run -Dspring-boot.run.arguments=--server.port=8080

# 新模块冒烟：追溯 / 召回 / 防错 / 扫码采集 / 不合格品处置 / 箱托盘码 / 质量分析
python back_code/scripts/smoke-test-new-modules.py http://localhost:8080/api/v1

# 原有全流程回归：地块 → 种植 → 入库 → 质检 → 加工 → 赋码 → 激活 → 监管
pwsh back_code/scripts/smoke-test.ps1 -BaseUrl http://localhost:8080/api/v1

# 复位冒烟数据（清记录 + 恢复被冻结的测试批次）
mysql -uroot -p qukuailian < back_code/scripts/reset-smoke-data.sql

# 权限一致性
pwsh back_code/scripts/check-permission-parity.ps1
```

## 相关文档

- [部署指南](docs/部署指南.md) — 开发/生产/Docker 三种部署方式 + 常见问题
- [数据库初始化指南](docs/数据库初始化指南.md) — MySQL 建库建表 + 种子账号
- [FISCO BCOS 搭建指南](docs/FISCO_BCOS搭建指南.md) — 区块链节点搭建 + 合约部署
- [需求对照与改进说明](需求对照与改进说明.md) — 需求逐条对照矩阵、补齐内容、联调与冒烟测试记录

## License

MIT
