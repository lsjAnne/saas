# 抖音云部署 · Spring Boot 后端容器镜像
# 基础镜像：Java 17 运行时（抖音云容器服务支持任意语言框架）
FROM eclipse-temurin:17-jre-jammy

# 时区与编码
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

WORKDIR /app

# 复制 Spring Boot 可执行 jar（由 CI/构建机 mvn package 产出）
ARG JAR_FILE=target/dianShangPingTai-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 服务端口（抖音云容器服务可配置）
EXPOSE 8080

# 健康检查（Spring Boot Actuator 已暴露 /actuator/health）
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

# 启动：prod profile 由抖音云环境变量注入（SPRING_PROFILES_ACTIVE）
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
