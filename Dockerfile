# 多阶段构建 Dockerfile for cqwu_appointment_system
# 阶段1: 构建阶段 - 使用 Maven 编译项目
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 复制 pom.xml 和 Maven wrapper
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 下载依赖（利用 Docker 缓存层）
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src ./src

# 编译打包（跳过测试以加快构建）
RUN ./mvnw clean package -DskipTests -B

# 阶段2: 运行阶段 - 使用轻量级 JRE 镜像
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 创建非 root 用户运行应用
RUN addgroup -S spring && adduser -S spring -G spring

# 从构建阶段复制编译好的 jar 包
COPY --from=builder /build/target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs && chown -R spring:spring /app

# 切换到非 root 用户
USER spring:spring

# 暴露应用端口
EXPOSE 8080

# JVM 参数优化
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
