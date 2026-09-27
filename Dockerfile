# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache dependency resolution separately from source changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY src ./src
RUN ./mvnw -B clean package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/astrabit-assignment-*.jar app.jar

# Render (and most PaaS hosts) inject PORT at runtime and expect the app to
# bind to it - see server.port=${PORT:8080} in application.yaml.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
