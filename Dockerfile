FROM maven:3.9.8-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/target/dianShangPingTai-1.0.0-SNAPSHOT.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=postgres
ENV APP_AUTH_TOKEN_SECRET=change-me-before-production
ENV APP_AUTH_BOOTSTRAP_PASSWORD=change-me-before-production
ENV APP_AUTH_REQUIRE_EXPLICIT_SECRETS=true
ENV APP_AUTH_ALLOW_LEGACY_HEADER_CONTEXT=false

EXPOSE 8080

# 健康检查走 liveness 探针（不依赖 Redis/Postgres，避免外部组件未就绪时误判容器死亡）
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q --spider http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
