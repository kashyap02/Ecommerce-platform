# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .

COPY user-service/pom.xml          user-service/
COPY product-service/pom.xml       product-service/
COPY order-service/pom.xml         order-service/
COPY payment-service/pom.xml       payment-service/
COPY notification-service/pom.xml  notification-service/
COPY rate-limiter-service/pom.xml  rate-limiter-service/
COPY api-gateway/pom.xml           api-gateway/

RUN mvn dependency:go-offline -pl user-service -am -q

COPY user-service/src user-service/src

RUN mvn package -pl user-service -am -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/user-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]