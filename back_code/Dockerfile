# =====================================================================
# 多阶段构建：Maven 构建 -> JRE 运行
# =====================================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先拷贝 pom.xml 拉取依赖，利用 Docker 层缓存加速后续构建
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar
# FISCO BCOS SDK 配置文件与证书目录（bcos/config.toml、bcos/conf/、bcos/account/）
COPY --from=build /build/src/main/resources/bcos ./bcos

EXPOSE 8080

ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-jar", "app.jar"]
