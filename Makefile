.PHONY: up down build-images logs infra-up infra-down backend frontend frontend-install typecheck backend-build backend-test build

## Start full stack via docker-compose (infra + Judge0 + backend + frontend)
up:
	docker compose -f infra/docker-compose.yml up -d --build

## Stop full stack
down:
	docker compose -f infra/docker-compose.yml down

## Rebuild backend/frontend images
build-images:
	docker compose -f infra/docker-compose.yml build

## Tail logs for all services
logs:
	docker compose -f infra/docker-compose.yml logs -f

## Start only infra services (postgres, redis, rabbitmq, judge0) for native dev
infra-up:
	docker compose -f infra/docker-compose.yml up -d postgres redis rabbitmq judge0-db judge0-redis judge0-server judge0-workers mailhog

## Stop infra services
infra-down:
	docker compose -f infra/docker-compose.yml down

## Run backend (native dev)
backend:
	cd backend && ./mvnw spring-boot:run

## Run frontend (native dev)
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

## Full clean build (native)
build: backend-build
	cd frontend && npm run build
