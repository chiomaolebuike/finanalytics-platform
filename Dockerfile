# Stage 1: Build — full JDK + Maven
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q        # cache dependencies as Docker layer
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime — lean JRE only (~180MB vs ~600MB with full JDK)
# Smaller image = smaller attack surface = faster pulls
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Non-root user — PCI DSS Req 7 (least privilege)
RUN addgroup -S finanalytics && adduser -S app -G finanalytics
USER app

COPY --from=builder /build/target/transaction-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]