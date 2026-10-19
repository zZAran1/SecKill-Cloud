# ===== 构建阶段：Maven 打包 =====
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 多模块工程整体拷贝（common 是各服务的本地依赖，必须一起构建）
COPY . .

# -pl 只打包当前模块，-am 连带构建它依赖的模块；跳过测试
RUN mvn -q -pl SecKill-gateway -am package -DskipTests

# ===== 运行阶段：JRE 21 运行 =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Spring Boot 打出的可执行 fat jar（.original 不会被 *.jar 匹配到）
COPY --from=build /workspace/SecKill-gateway/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]