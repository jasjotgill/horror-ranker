# Builds one image that serves both the API and the React app.
# Build from the repository root, because it needs frontend/ as well as backend/:
#   docker build -f backend/Dockerfile .

# Stage 1: build the React app into static files.
FROM node:24-alpine AS frontend
WORKDIR /app/frontend
# Copy only the dependency lists first. Docker caches each step, so "npm ci" is
# re-run only when these files change, not on every source edit.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: build the Spring Boot jar with a full JDK.
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app/backend
# Same caching idea: download dependencies before copying the source.
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src/ src/
# Spring Boot serves anything in static/ at the site root, so the React build
# becomes part of the jar: one container, one URL, no CORS.
COPY --from=frontend /app/frontend/dist/ src/main/resources/static/
# Tests need Docker (Testcontainers), which is not available inside a build. CI runs them.
RUN ./mvnw -B -q package -DskipTests

# Stage 3: the image that actually runs. A JRE is enough to run a jar,
# and nothing from the build stages (Maven, Node, source code) ends up in it.
FROM eclipse-temurin:21-jre
WORKDIR /app
# Do not run as root inside the container.
RUN useradd --system --no-create-home app
USER app
COPY --from=backend /app/backend/target/*.jar app.jar
EXPOSE 8080
# Let the JVM use most of the container's memory limit, which matters on small hosts.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
