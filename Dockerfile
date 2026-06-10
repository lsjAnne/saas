FROM maven:3.9.8-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/target/dianShangPingTai-1.0.0-SNAPSHOT.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=postgres
ENV APP_AUTH_TOKEN_SECRET=change-me-before-production
ENV APP_AUTH_BOOTSTRAP_PASSWORD=change-me-before-production
ENV APP_AUTH_REQUIRE_EXPLICIT_SECRETS=true
ENV APP_AUTH_ALLOW_LEGACY_HEADER_CONTEXT=false

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
