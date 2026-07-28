# rice-trace-blockchain

本目录根据 `基于区块链的水稻产品质量安全溯源系统接口文档.md` 实现 Vue3 管理端与移动端溯源演示，支持 Mock 数据和真实 API 环境切换。

## 前端联调交付

- 演示脚本：`docs/前端联调演示脚本.md`
- 问题清单：`docs/前端联调问题清单.md`

## 目录说明

```text
back_code   后端预留目录，当前不写后端代码
front_code  Vue3 前端框架
```

## 开发约定

1. 前端管理端页面路由以 `/rice` 为模块前缀。
2. 小程序页面占位放在 `front_code/src/views/mini` 中，后续可迁移到 Uni-app 或微信小程序工程。
3. 后端暂不搭建，`back_code` 只保留占位说明，后续需要时再补 Spring Boot 工程。
4. 详细字段、校验规则、状态流转以接口文档和开发文档为准。
