.PHONY: up down build test run api
up:        ## Start local infra
	docker compose up -d
	sleep 5
	docker compose ps

e2e: test  ## Alias

down:      ## Stop local infra
	docker compose down -v

build:     ## Build all modules
	./gradlew clean build -x test

test:      ## Run unit/integration tests
	./gradlew test

run:       ## Run api-gateway
	./gradlew :api-gateway:bootRun

api:       ## Open Swagger UI
	python3 -c "import webbrowser; webbrowser.open('http://localhost:8080/swagger-ui.html')"
