PROJECT_NAME := psych-api
QUARKUS_VERSION := 3.37.1
JAVA_VERSION := 21

.PHONY: all build test clean package dev docker-build docker-run docker-native-build docker-native-run

all: build test

build:
	@echo "Building $(PROJECT_NAME)..."
	mvn clean install -DskipTests

test:
	@echo "Running tests for $(PROJECT_NAME)..."
	mvn test

clean:
	@echo "Cleaning $(PROJECT_NAME)..."
	mvn clean

package:
	@echo "Packaging $(PROJECT_NAME)..."
	mvn package

dev:
	@echo "Starting $(PROJECT_NAME) in dev mode..."
	mvn quarkus:dev

# Docker JVM
docker-build-jvm:
	@echo "Building JVM Docker image for $(PROJECT_NAME)..."
	docker build -f src/main/docker/Dockerfile.jvm -t $(PROJECT_NAME):jvm .

docker-run-jvm:
	@echo "Running JVM Docker container for $(PROJECT_NAME)..."
	docker run -i --rm -p 8080:8080 --network psych-network $(PROJECT_NAME):jvm

# Docker Native
docker-build-native:
	@echo "Building Native Docker image for $(PROJECT_NAME)..."
	mvn package -Dnative -Dquarkus.native.container-build=true
	docker build -f src/main/docker/Dockerfile.native -t $(PROJECT_NAME):native .

docker-run-native:
	@echo "Running Native Docker container for $(PROJECT_NAME)..."
	docker run -i --rm -p 8080:8080 --network psych-network $(PROJECT_NAME):native

# Helper for local development with docker-compose (assuming docker-compose.yml is in parent dir)
compose-up:
	@echo "Starting all services via docker-compose..."
	cd .. && docker compose up --build -d

compose-down:
	@echo "Stopping all services via docker-compose..."
	cd .. && docker compose down
