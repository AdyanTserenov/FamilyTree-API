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
RUN mvn -pl family-tree-auth-starter,tree-service dependency:go-offline -q 2>/dev/null || true

# Copy sources
COPY family-tree-auth-starter/src /build/family-tree-auth-starter/src
COPY tree-service/src /build/tree-service/src

# Install starter into local Maven repo, then build tree-service
RUN mvn -pl family-tree-auth-starter install -DskipTests -q && \
    mvn -pl tree-service package -DskipTests -q

# ── Runtime image ──────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/tree-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
