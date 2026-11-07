# Stage 1: Build and package
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lightweight OCI runtime image
FROM eclipse-temurin:21.0.13_9-jre-alpine
WORKDIR /app
RUN addgroup -S styleradar && adduser -S appuser -G styleradar
USER appuser
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8300
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
