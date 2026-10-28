# ---------- Etapa de build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# El padre declara ambos módulos, así que Maven necesita los dos pom.xml
COPY pom.xml .
COPY support-ticket-service/pom.xml support-ticket-service/
COPY technician-dispatch-service/pom.xml technician-dispatch-service/

COPY support-ticket-service/src support-ticket-service/src

# Caché de dependencias entre builds (requiere BuildKit, activo por defecto)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl support-ticket-service -am package -DskipTests

# ---------- Etapa de ejecución ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /workspace/support-ticket-service/target/*.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]