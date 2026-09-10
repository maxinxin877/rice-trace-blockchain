# =====================================================================
# 前端镜像：Node 构建 -> Nginx 托管静态资源 + 反向代理后端
# 构建上下文：front_code 目录
#   docker build -t rice-trace-frontend .
# 说明：镜像内构建前端产物，服务器无需安装 Node。
# =====================================================================

# ---------- 构建阶段 ----------
FROM node:22-alpine AS build
WORKDIR /build

# 国内服务器建议使用镜像源；可用 build-arg 覆盖为官方源：
#   --build-arg NPM_REGISTRY=https://registry.npmjs.org
ARG NPM_REGISTRY=https://registry.npmmirror.com

# 先装依赖，利用层缓存（package.json 不变时不会重复安装）
COPY package.json package-lock.json ./
RUN npm config set registry "$NPM_REGISTRY" \
    && npm ci --no-audit --no-fund

COPY . .

# 生产构建：vue-tsc 类型检查 + vite build -> /build/dist
# 生产环境变量来自 .env.production（VITE_USE_MOCK=false、VITE_API_BASE_URL=/api/v1）
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine

# 站点配置（SPA 回退 + /api 反向代理 + 上传体积限制）
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 前端构建产物
COPY --from=build /build/dist /usr/share/nginx/html

ENV TZ=Asia/Shanghai
EXPOSE 80

# nginx 官方镜像自带健康检查命令
HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
    CMD wget -qO- http://127.0.0.1/healthz || exit 1
