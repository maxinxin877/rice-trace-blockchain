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
│   │   │       ├── trace/              # 窜货预警
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
│   │   ├── controller/         # 13 个 REST Controller
│   │   ├── service/impl/       # 13 个业务 Service
│   │   ├── mapper/             # 18 个 MyBatis Mapper
│   │   ├── entity/             # 18 个 数据库实体
│   │   ├── dto/ vo/            # 请求/响应 DTO
│   │   ├── enums/              # 状态枚举
│   │   ├── config/             # WebMvc/Redis/MyBatisPlus 配置
│   │   ├── security/           # JWT 工具类
│   │   └── QuukuailianApplication.java
│   ├── src/main/resources/
│   │   ├── mapper/*.xml        # MyBatis XML
│   │   └── application*.yml    # 配置文件 (dev/test/prod)
│   ├── sql/init.sql            # 数据库初始化脚本 (18 表 + 6 种子账号)
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
| 防伪溯源 | 防伪码生成/激活、扫码记录、窜货预警 | 品牌运营 |
| 监管审计 | 产量平衡校验、风险预警处理、审计日志、链上存证 | 监管人员 |
| 数据看板 | 8 项统计卡片 + ECharts 图表 | 全部角色 |

### 小程序扫码端

| 页面 | 功能 |
|---|---|
| 扫码溯源 | 产品详情 + 全链路溯源时间轴 |
| 真伪鉴别 | 正品/未激活/窜货/假冒 四级判定 |
| 认证证书 | 绿色食品/有机/质检报告 |
| 链上核验 | 区块高度、交易哈希、链上摘要比对 |

### 核心业务流程

```
地块 → 种植批次 → 农事记录/环境监测 → 入库质检 → 加工 → 成品 → 防伪码 → 扫码溯源
 🌾      🌱          📝 🌡️            📦 🔬     ⚙️    🍚     🔐      📱
```

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

后端 13 个 Controller 提供 RESTful API，统一前缀 `/api/v1`：

| 路径前缀 | Controller | 功能 |
|---|---|---|
| `/auth` | AuthController | 登录认证、用户信息 |
| `/rice/fields` | RiceFieldController | 地块 CRUD |
| `/rice/planting-batches` | RicePlantingBatchController | 种植批次 |
| `/rice/farming-logs` | RiceFarmingLogController | 农事记录 |
| `/rice/environment-records` | RiceEnvironmentRecordController | 环境监测 |
| `/rice/milling-batches` | RiceMillingBatchController | 加工批次 |
| `/rice/storage-receipts` | RiceStorageReceiptController | 入库单 |
| `/rice/product-batches` | RiceProductBatchController | 成品批次 |
| `/rice/trace-codes` | RiceTraceCodeController | 防伪码 |
| `/rice/channel-warnings` | RiceChannelWarningController | 窜货预警 |
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

## 相关文档

- [部署指南](docs/部署指南.md) — 开发/生产/Docker 三种部署方式 + 常见问题
- [数据库初始化指南](docs/数据库初始化指南.md) — MySQL 建库建表 + 种子账号
- [FISCO BCOS 搭建指南](docs/FISCO_BCOS搭建指南.md) — 区块链节点搭建 + 合约部署

## License

MIT
