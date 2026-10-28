# ===== 构建阶段：Maven 打包 =====
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Maven 依赖走阿里云镜像源，国内下载提速
COPY docker/maven-settings.xml /root/.m2/settings.xml

# 先只拷贝 pom.xml 预下载依赖：依赖层独立缓存，改业务代码不重复下载依赖
COPY pom.xml .
COPY SecKill-common/pom.xml SecKill-common/
COPY SecKill-user/pom.xml SecKill-user/
COPY SecKill-product/pom.xml SecKill-product/
COPY SecKill-order/pom.xml SecKill-order/
COPY SecKill-gateway/pom.xml SecKill-gateway/
RUN --mount=type=cache,target=/root/.m2/repository mvn -q -pl SecKill-order -am dependency:go-offline -DskipTests

# 再拷贝源码编译打包（-pl 只打包当前模块，-am 连带构建它依赖的模块；跳过测试）
COPY . .
RUN --mount=type=cache,target=/root/.m2/repository mvn -q -pl SecKill-order -am package -DskipTests

# ===== 运行阶段：JRE 21 运行 =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Spring Boot 打出的可执行 fat jar（.original 不会被 *.jar 匹配到）
COPY --from=build /workspace/SecKill-order/target/*.jar app.jar

# JVM 参数：堆上限按容器内存的 75% 自动推算（配合 docker-compose 的 mem_limit），G1 回收器，OOM 时落 heap dump
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"

EXPOSE 8083
# exec 保证 java 成为 PID 1，停容器时能收到 SIGTERM 优雅停机；JAVA_OPTS 可在 docker-compose 里覆盖
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
