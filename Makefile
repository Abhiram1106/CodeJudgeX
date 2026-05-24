.PHONY: infra-up infra-down backend frontend lint test

## Start all infrastructure services
infra-up:
	docker compose -f infra/docker-compose.yml up -d

## Stop all infrastructure services
infra-down:
	docker compose -f infra/docker-compose.yml down

## Run backend (dev)
backend:
	cd backend && ./mvnw spring-boot:run

## Run frontend (dev)
frontend:
	cd frontend && npm run dev

## Install frontend deps
frontend-install:
	cd frontend && npm install

## Typecheck frontend
typecheck:
	cd frontend && npm run typecheck

## Build backend jar
backend-build:
	cd backend && ./mvnw clean package -DskipTests

## Run backend tests
backend-test:
	cd backend && ./mvnw test

## Full clean build
build: backend-build
	cd frontend && npm run build
