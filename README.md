# rice-trace-blockchain

基于区块链的水稻产品质量安全溯源系统。

```text
back_code   Spring Boot 后端（MyBatis-Plus + MySQL + Redis + FISCO BCOS SDK）
front_code  Vue3 管理端（Element Plus + ECharts + Leaflet），含小程序页面占位
```

## 快速开始

```bash
# 数据库：先建基础表，再按顺序执行两个增量脚本
mysql -uroot -p qukuailian < back_code/sql/init.sql
mysql -uroot -p qukuailian < back_code/sql/trace_recall_module.sql
mysql -uroot -p qukuailian < back_code/sql/collect_quality_module.sql

# 后端
cd back_code && mvn spring-boot:run

# 前端
cd front_code && npm install && npm run dev
```

## 功能模块

| 分组 | 模块 |
|---|---|
| 主业务流程 | 数据看板、地块档案、种植批次、农事记录、环境数据、扫码采集、收储入库、碾米加工、成品批次 |
| 监管与风控 | 追溯查询、防伪码管理、包装编码（箱/托盘）、窜货预警、召回管理、不合格品处置、质量分析、风险预警、审计日志、产量平衡、链上存证 |
| 小程序（占位） | 扫码溯源、真伪鉴别、认证证书、链上核验 |

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

## 开发约定

1. 前端管理端页面路由以 `/rice` 为模块前缀。
2. 小程序页面占位放在 `front_code/src/views/mini`，后续可迁移到 Uni-app 或微信小程序工程。
3. 字段、校验规则、状态流转以接口文档、开发文档与《需求对照与改进说明.md》为准。
4. 新增权限码必须同时改 `PermissionConstants`、`stores/user.ts`、菜单与路由，
   并执行 `back_code/scripts/check-permission-parity.ps1` 校验。

## 相关文档

- `需求对照与改进说明.md`：需求文档逐条对照矩阵、本次补齐内容、待补清单、**联调与冒烟测试记录**
- `back_code/docs/`：FISCO BCOS 搭建、数据库初始化、部署指南

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
