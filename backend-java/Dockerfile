# Multi-stage build for VietERP Spring Boot modules
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY settings.xml . 2>/dev/null || true
COPY backend-java/pom.xml ./backend-java/
COPY backend-java/module-hrm ./backend-java/module-hrm/
COPY backend-java/module-events ./backend-java/module-events/
COPY backend-java/module-auth ./backend-java/module-auth/
COPY backend-java/module-api-types ./backend-java/module-api-types/
COPY backend-java/module-shared ./backend-java/module-shared/
WORKDIR /app/backend-java
RUN ./mvnw package -DskipTests -pl module-hrm -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/backend-java/module-hrm/target/module-hrm-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]