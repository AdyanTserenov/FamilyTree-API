FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Install Maven
RUN apk add --no-cache maven

# Copy parent pom + module poms first (layer caching for dependencies)
COPY pom.xml /build/pom.xml
COPY family-tree-auth-starter/pom.xml /build/family-tree-auth-starter/pom.xml
COPY auth-service/pom.xml /build/auth-service/pom.xml
COPY tree-service/pom.xml /build/tree-service/pom.xml

# Download dependencies (cached layer)
RUN mvn -pl family-tree-auth-starter,auth-service dependency:go-offline -q 2>/dev/null || true

# Copy sources
COPY family-tree-auth-starter/src /build/family-tree-auth-starter/src
COPY auth-service/src /build/auth-service/src

# Install starter into local Maven repo, then build auth-service
RUN mvn -pl family-tree-auth-starter install -DskipTests -q && \
    mvn -pl auth-service package -DskipTests -q

# ── Runtime image ──────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
