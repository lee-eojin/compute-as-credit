# Compute-as-Credit

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)

> **This is a portfolio and demonstration project.**
>
> **Multi-Provider GPU Orchestrator with Credit-based Billing**
>
> Intelligent job scheduling across multiple compute providers with double-entry accounting

> 본 프로젝트는 포트폴리오 및 데모 프로젝트용입니다.
>
> 멀티 프로바이더 GPU 오케스트레이터 및 크레딧 기반 과금 시스템
>
> 복수 컴퓨팅 프로바이더 간 지능형 작업 스케줄링 및 복식부기 회계 처리

A production-ready Spring Boot microservices platform for managing AI/ML workloads across multiple cloud GPU providers (RunPod, AWS, GCP, etc.) with automatic cost optimization, credit-based billing, and comprehensive observability.

RunPod, AWS, GCP 등 다수의 클라우드 GPU 프로바이더를 통합 관리하는 프로덕션 레벨 Spring Boot 마이크로서비스 플랫폼. 자동 비용 최적화, 크레딧 기반 과금, 포괄적 관찰성 제공.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Development](#development)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Features

### Core Capabilities
- **Multi-Provider Orchestration**: Automatically select optimal GPU provider based on cost, latency, and reliability
- **Credit-based Billing**: Double-entry accounting ledger with hold/debit/refund transactions
- **Job Lifecycle Management**: Submit → Queue → Provision → Run → Complete with full state tracking
- **Provider Abstraction**: Plug & play adapter pattern for adding new compute providers
- **Security**: JWT-based authentication with OAuth2 resource server and scope-based RBAC
- **Idempotency**: Built-in idempotency key support for safe retry of API requests
- **Event-Driven**: Transactional outbox pattern with RabbitMQ for reliable event delivery
- **Observability**: Swagger/OpenAPI documentation, structured logging, and metrics

### Production-Ready Components
- **API Gateway**: REST endpoints with validation, security, and documentation
- **Orchestrator**: Quote aggregation, provider selection, and job state machine
- **Billing Module**: Ledger-based accounting with ACID guarantees
- **Adapters**: RunPod integration (fake adapter for testing)
- **Storage Service**: Presigned URL generation for S3/blob storage I/O
- **Reconciliation**: Periodic sync to detect and recover stuck jobs

## Architecture

### Module Structure

```
compute-as-credit/
├── api-gateway          # REST API + Security + Swagger
├── orchestrator         # Job lifecycle + Provider selection
├── billing              # Double-entry ledger
├── domain               # Core entities + JPA repositories
├── shared               # RabbitMQ config + Events
├── adapters-core        # Provider client interface
├── adapters-fake        # Mock provider for testing
├── adapters-runpod      # RunPod API integration
└── agent-sdk            # Client library for AI agents
```

### Data Flow

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│  API Gateway│─────▶│ Orchestrator │─────▶│   Billing   │
│  (REST+JWT) │      │ (Selection)  │      │  (Ledger)   │
└─────────────┘      └──────────────┘      └─────────────┘
       │                     │                      │
       │              ┌──────▼──────┐               │
       │              │  Providers  │               │
       │              │ (RunPod etc)│               │
       │              └─────────────┘               │
       │                                            │
       └──────────────▶  MySQL  ◀───────────────────┘
                          │
                    ┌─────▼─────┐
                    │  RabbitMQ │
                    │  (Events) │
                    └───────────┘
```

### Key Design Patterns
- **Hexagonal Architecture**: Domain-driven design with ports & adapters
- **Transactional Outbox**: Reliable event publishing without distributed transactions
- **Circuit Breaker**: Resilience4j for fault tolerance (WIP)
- **Strategy Pattern**: Pluggable provider selection policies (BalancedPolicy, etc.)

## Prerequisites

- **Java 17+** (tested with Temurin 17.0.9)
- **Docker** & Docker Compose (for MySQL + RabbitMQ)
- **Gradle 8.5+** (wrapper included)
- **Git** (for version control)

### Optional (for development)
- **IntelliJ IDEA** or any Java IDE
- **Postman** or `curl` for API testing
- **Python 3** (for JWT token generation script)

## Quick Start

### 1. Clone & Setup

```bash
git clone <repository-url>
cd compute-as-credit
```

### 2. Start Infrastructure

```bash
# Start MySQL + RabbitMQ
make up

# Verify containers are running
docker compose ps
```

### 3. Build & Run

```bash
# Build all modules
./gradlew clean build -x test

# Run API Gateway (http://localhost:8080)
make run
# OR
./gradlew :api-gateway:bootRun
```

### 4. Explore API

Open Swagger UI: **http://localhost:8080/swagger-ui.html**

Or via curl:
```bash
# Generate JWT token (for development)
# Use https://jwt.io to create a token with:
# - Algorithm: HS256
# - Secret: dev-secret
# - Payload: {"sub": "user1", "scope": "jobs:read jobs:write"}
export TOKEN="your-generated-jwt-token"

# Submit a job
curl -X POST http://localhost:8080/v1/jobs \
  -H "Authorization: Bearer <TOKEN_FROM_ABOVE>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "userId": 1,
    "agentSpec": "{\"image\":\"ghcr.io/your-org/agent:1.0\",\"cmd\":[\"python\",\"train.py\"]}",
    "resourceHint": "{\"gpuType\":\"A100-80G\",\"spotOk\":true}",
    "maxBudget": 50.0
  }'

# Get job status
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/v1/jobs/1
```

## Development

### Project Structure

```
src/main/java/com/yourco/compute/
├── api/                    # API Gateway
│   ├── controller/         # REST endpoints (JobController)
│   ├── security/           # JWT + OAuth2 (SecurityConfig)
│   ├── dto/                # JobApiModels (SubmitReq, SubmitRes, JobRes)
│   └── infra/              # IdempotencyService
├── orchestrator/
│   ├── service/            # JobOrchestrator (core orchestration logic)
│   ├── quotes/             # QuoteService (provider price aggregation)
│   ├── selector/           # SelectionPolicy + BalancedPolicy
│   ├── outbox/             # OutboxPublisher (RabbitMQ events)
│   ├── storage/            # StorageService (S3 presigned URLs)
│   ├── usage/              # UsagePollingService (periodic polling)
│   └── reconcile/          # Reconciler (stuck job recovery)
├── billing/
│   └── ledger/             # LedgerEntities, LedgerRepos, LedgerService
├── domain/
│   ├── model/              # Job, JobStatus, Provider, OutboxEvent
│   └── repo/               # JobRepository, OutboxEventRepository
└── shared/
    ├── events/             # DomainEvents (JobSubmitted, JobStarted, etc.)
    └── messaging/          # RabbitConfig (exchange + queue setup)
```

### Running Tests

```bash
# Run all tests (requires Testcontainers)
./gradlew test

# Run specific module tests
./gradlew :api-gateway:test

# Skip tests during build
./gradlew build -x test
```

### Database Migrations

Flyway migrations are in `domain/src/main/resources/db/migration/`:
```
V1__init.sql    # Initial schema (jobs, providers, ledger, outbox, etc.)
```

Migrations run automatically on application startup.

### Adding a New Provider

1. Create adapter in `adapters-{provider}/`
2. Implement `ProviderClient` interface
3. Add `@Component` annotation
4. Update `QuoteService` to fetch quotes
5. Add integration test with WireMock

## API Documentation

### Authentication

All endpoints require JWT Bearer token with appropriate scopes:
- `jobs:read` - View job status
- `jobs:write` - Submit jobs

**Token Generation** (development only):

Visit [jwt.io](https://jwt.io) and create a token with:
- Algorithm: `HS256`
- Secret: `dev-secret`
- Payload:
  ```json
  {
    "sub": "user1",
    "scope": "jobs:read jobs:write",
    "exp": 9999999999
  }
  ```

### Endpoints

#### Submit Job
```http
POST /v1/jobs
Content-Type: application/json
Authorization: Bearer {token}
Idempotency-Key: {unique-key}  # Optional

{
  "userId": 1,
  "agentSpec": "{\"image\":\"...\"}",  # JSON string
  "resourceHint": "{\"gpuType\":\"A100-80G\"}",  # JSON string
  "maxBudget": 100.0
}

Response: 200 OK
{
  "jobId": 123,
  "status": "QUEUED"
}
```

#### Get Job
```http
GET /v1/jobs/{id}
Authorization: Bearer {token}

Response: 200 OK
{
  "jobId": 123,
  "status": "RUNNING",
  "providerId": 5
}
```

#### Allocate I/O URLs
```http
POST /v1/jobs/{id}/io
Authorization: Bearer {token}

Response: 200 OK
{
  "uploadUrl": "https://...",
  "downloadUrl": "https://...",
  "inputUri": "s3://tenant/123/input/",
  "outputUri": "s3://tenant/123/output/",
  "expiresAt": "2025-10-02T12:00:00Z"
}
```

### Job Lifecycle

```
SUBMITTED → QUEUED → PROVISIONING → RUNNING → SUCCEEDED
                                          ↓
                                       FAILED
                                          ↓
                                     CANCELLED
```

## Testing

### Integration Tests

```bash
# API Gateway integration test (Testcontainers)
./gradlew :api-gateway:test

# WireMock test for RunPod adapter
./gradlew :adapters-runpod:test
```

### Manual Testing with Postman

1. Import Swagger spec: `http://localhost:8080/v3/api-docs`
2. Set Authorization: Bearer token (from jwt.io)
3. Test endpoints

## Deployment

### Docker Compose (Development)

```bash
# Already includes MySQL + RabbitMQ
docker compose up -d
```

### Production Considerations

1. **Database**: Use managed MySQL (AWS RDS, Cloud SQL)
2. **Message Queue**: Use managed RabbitMQ (CloudAMQP) or switch to Kafka
3. **Secrets**: Use AWS Secrets Manager / Vault (not `.env`)
4. **JWT Secret**: Generate strong 256-bit key
5. **Monitoring**: Add Prometheus + Grafana
6. **Logging**: Use structured JSON logs → ELK/Splunk
7. **High Availability**: Deploy multiple API Gateway instances behind load balancer

### Environment Variables

```bash
# Database
DB_URL=jdbc:mysql://localhost:3306/compute
DB_USER=root
DB_PASS=root

# RabbitMQ
RABBIT_HOST=localhost
RABBIT_PORT=5672

# Security
JWT_SECRET=your-strong-256-bit-secret
```

## Troubleshooting

### Build Fails with "Could not find org.springframework.boot:..."

**Cause**: Gradle dependency resolution issue.

**Fix**:
```bash
./gradlew clean build --refresh-dependencies
```

### "Unable to locate a Java Runtime"

**Cause**: JAVA_HOME not set or wrong Java version.

**Fix**:
```bash
export JAVA_HOME=/path/to/java17
java -version  # Should show Java 17
```

### MySQL Connection Refused

**Cause**: Docker container not running or port conflict.

**Fix**:
```bash
docker compose ps  # Check if mysql container is up
docker compose logs mysql  # Check logs
lsof -i :3306  # Check if port 3306 is available
```

### RabbitMQ Connection Error

**Cause**: RabbitMQ not started or wrong credentials.

**Fix**:
```bash
docker compose ps  # Check if rabbitmq container is up
# Access management UI: http://localhost:15672
# Default credentials: guest/guest
```

### JWT Token Invalid

**Cause**: Token expired or wrong secret.

**Fix**:
```bash
# Regenerate token at https://jwt.io
# Paste token and verify with secret: dev-secret
# Ensure 'exp' claim is in the future
```

### Flyway Migration Fails

**Cause**: Schema already exists or migration checksum mismatch.

**Fix**:
```bash
# Drop and recreate database
docker compose down -v
docker compose up -d
# Wait 10s, then restart app
```

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'feat: add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Code Style
- Follow Spring Boot best practices
- Use meaningful variable names
- Add Javadoc for public APIs
- Write tests for new features

### Commit Convention
```
feat: Add new feature
fix: Bug fix
docs: Documentation update
refactor: Code refactoring
test: Add tests
chore: Build/config changes
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Spring Boot team for the excellent framework
- Testcontainers for integration testing
- WireMock for HTTP mocking
- All contributors and open-source maintainers

---

**Built using Java 17, Spring Boot 3, and modern cloud-native practices.**

For questions or support, please open an issue on GitHub.
