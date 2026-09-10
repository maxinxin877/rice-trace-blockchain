# =====================================================================
# 后端镜像：Maven 构建 -> JRE 运行
# 构建上下文：back_code 目录（docker compose 会自动使用该目录）
#   docker build -t rice-trace-backend .
# =====================================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Maven 镜像源：默认走阿里云公共仓库（国内云服务器速度差别很大）
# 需要改回中央仓库时：--build-arg MAVEN_MIRROR=https://repo.maven.apache.org/maven2
ARG MAVEN_MIRROR=https://maven.aliyun.com/repository/public
COPY docker/maven-settings.xml /root/.m2/settings.xml
RUN if [ -n "$MAVEN_MIRROR" ]; then \
        sed -i "s#<url>.*</url>#<url>${MAVEN_MIRROR}</url>#" /root/.m2/settings.xml; \
    fi

# 先只拷贝 pom.xml 并预拉依赖，利用 Docker 层缓存（源码改动时不必重新下载依赖）
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -DskipTests \
    || echo "[warn] dependency:go-offline 未完全成功，后续 package 阶段会补齐依赖"

# 再拷贝源码并打包
COPY src ./src
RUN mvn -B -DskipTests clean package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8"

COPY --from=build /build/target/*.jar app.jar
# FISCO BCOS SDK 配置文件与证书目录（bcos/config.toml、bcos/conf/、bcos/account/）
COPY --from=build /build/src/main/resources/bcos ./bcos

# 文件上传目录：docker-compose 挂载数据卷持久化，避免容器重建后照片丢失
RUN mkdir -p /app/upload
VOLUME ["/app/upload"]

EXPOSE 8080

# 通过 JAVA_OPTS 可在 compose 中调整堆大小等参数
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
