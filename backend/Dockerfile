# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Usamos el JDK completo solo para compilar. El JDK incluye herramientas de
# desarrollo (javac, jshell, etc.) que NO necesitamos en producción.
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
# La imagen final usa solo el JRE (Java Runtime Environment), que es ~200 MB
# más liviana que el JDK. alpine la reduce aún más (~60 MB base).
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/TravelAgency.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]