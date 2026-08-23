.PHONY: help dev build test run clean docker native

# Variables
APP_NAME := psych-api
VERSION := 1.0.0-SNAPSHOT
JAR_TARGET := target/quarkus-app/quarkus-run.jar
NATIVE_TARGET := target/${APP_NAME}-${VERSION}-runner

# Default target
help:
	@echo "Psych API - Quarkus Makefile"
	@echo ""
	@echo "Available targets:"
	@echo "  dev       - Run in development mode with hot reload"
	@echo "  build     - Build the application"
	@echo "  test      - Run tests"
	@echo "  run       - Run the built JAR"
	@echo "  clean     - Clean build artifacts"
	@echo "  docker    - Build Docker image (JVM)"
	@echo "  native    - Build native image (requires GraalVM)"
	@echo "  swagger   - Open Swagger UI in browser"

# Development mode dengan hot reload
dev:
	./mvnw quarkus:dev

# Build aplikasi
build: clean
	./mvnw clean package -DskipTests

# Run tests
test:
	./mvnw test

# Run aplikasi (setelah build)
run:
	java -jar $(JAR_TARGET)

# Clean build artifacts
clean:
	./mvnw clean

# Build Docker image (JVM)
docker: build
	docker build -f src/main/docker/Dockerfile.jvm -t $(APP_NAME):$(VERSION) .

# Build native image (memerlukan GraalVM)
native:
	./mvnw package -Pnative -DskipTests

# Buka Swagger UI di browser
swagger:
	@echo "Opening Swagger UI at http://localhost:8080/swagger-ui"
	@open http://localhost:8080/swagger-ui || xdg-open http://localhost:8080/swagger-ui || start http://localhost:8080/swagger-ui
