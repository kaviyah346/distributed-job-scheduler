# ─── Stage 1: Build the Spring Boot JAR ─────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper & POM
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Copy source code
COPY src src

# Build executable jar without running tests during image build
RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests

# ─── Stage 2: Minimal Production JRE Runtime ─────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root system user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built JAR from builder stage
COPY --from=builder /app/target/distributed-job-scheduler-*.jar app.jar

# Expose backend port
EXPOSE 8080

# Configure JVM memory and run
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
