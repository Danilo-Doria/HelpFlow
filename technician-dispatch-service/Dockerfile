# ---------- Etapa de build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY support-ticket-service/pom.xml support-ticket-service/
COPY technician-dispatch-service/pom.xml technician-dispatch-service/

COPY technician-dispatch-service/src technician-dispatch-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl technician-dispatch-service -am package -DskipTests

# ---------- Etapa de ejecución ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /workspace/technician-dispatch-service/target/*.jar app.jar

USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]