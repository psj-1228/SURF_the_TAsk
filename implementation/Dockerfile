# syntax=docker/dockerfile:1

FROM gradle:8.10.2-jdk17 AS build

WORKDIR /workspace

COPY settings.gradle build.gradle ./
COPY src ./src

RUN gradle clean bootJar --no-daemon \
    && JAR_FILE="$(find build/libs -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" \
    && cp "$JAR_FILE" app.jar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN useradd --system --user-group --home-dir /app spring

COPY --from=build --chown=spring:spring /workspace/app.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=local

EXPOSE 8080

USER spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
