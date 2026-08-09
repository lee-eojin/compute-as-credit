# Compute-as-Credit

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)

> **This is a portfolio and demonstration project.**
>
> **Multi-Provider GPU Orchestrator with Credit-based Billing**
>
> Intelligent job scheduling across multiple compute providers with double-entry accounting

A production-ready Spring Boot microservices platform for managing AI/ML workloads across multiple cloud GPU providers (RunPod, AWS, GCP, etc.) with automatic cost optimization, credit-based billing, and comprehensive observability.

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

## Architecture

### Module Structure

```
compute-as-credit/
├── domain       # Core entities, JPA repositories, RabbitMQ config, events
├── adapters     # ProviderClient interface + RunPod & fake implementations
├── app          # REST API, orchestration, billing, security (Spring Boot main)
└── agent-sdk    # Client library for AI agents
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
./gradlew :app:bootRun
```

### 4. Explore API

Open Swagger UI: **http://localhost:8080/swagger-ui.html**

Or via curl:
```bash
# Generate JWT token (for development)
# Use https://jwt.io to create a token with:
# - Algorithm: HS256
# - Secret: dev-secret
# - Payload: {"sub": "1", "scope": "jobs:read jobs:write"}
#   'sub' is the numeric user id the job is billed to.
export TOKEN="your-generated-jwt-token"

# Submit a job
curl -X POST http://localhost:8080/v1/jobs \
  -H "Authorization: Bearer <TOKEN_FROM_ABOVE>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "agentSpec": "{\"image\":\"ghcr.io/your-org/agent:1.0\",\"cmd\":[\"python\",\"train.py\"]}",
    "resourceHint": "{\"region\":\"us-east-1\",\"gpuType\":\"A100-80G\",\"spotOk\":true}",
    "maxBudget": 50.0
  }'

# Get job status
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/v1/jobs/1
```

## Development

### Key Components by Module

**domain/**
- `Job`, `JobStatus`, `Provider`, `OutboxEvent` - Core entities
- `JobRepository`, `OutboxEventRepository` - JPA repositories
- `DomainEvents` - Event records (JobSubmitted, JobStarted, etc.)
- `RabbitConfig` - Exchange + queue setup

**adapters/**
- `ProviderClient` - Provider abstraction interface
- `RunPodClient` - RunPod API integration
- `FakeProviderClient` - Mock for testing

**app/**
- `JobController` - REST endpoints (submit, get, allocate I/O)
- `SecurityConfig` - JWT + OAuth2 resource server
- `JobApiModels` - DTO records (SubmitReq, SubmitRes, JobRes)
- `IdempotencyService` - Request deduplication
- `JobOrchestrator` - Core job lifecycle management
- `QuoteService` - Provider price aggregation
- `SelectionPolicy` + `BalancedPolicy` - Provider selection
- `OutboxPublisher` - RabbitMQ event publishing
- `StorageService` - S3 presigned URL generation
- `UsagePollingService` - Periodic usage polling (stub, not implemented)
- `LedgerService` - Double-entry accounting logic

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :app:test

# Skip tests during build
./gradlew build -x test
```

### Database Migrations

Flyway migrations are in `domain/src/main/resources/db/migration/`:
```
V1__init.sql                  # Initial schema (jobs, providers, ledger, outbox, etc.)
V2__provider_registry.sql     # Unique provider names + seed rows for the shipped adapters
V3__idempotency_per_user.sql  # Idempotency keys scoped to the user who sent them
V4__ledger_account_names.sql  # Named ledger accounts, so balance and hold stop colliding
```

Migrations run automatically on application startup.

### Adding a New Provider

1. Add adapter class in `adapters/`
2. Implement `ProviderClient` interface
3. Add `@Component` annotation
4. Update `QuoteService` to fetch quotes
5. Add a `providers` row via a Flyway migration, named after the adapter class
   (the orchestrator resolves `jobs.provider_id` by that name and fails the submit if no row exists)
6. Add a test for the adapter (`adapters` has no test source set yet)

## API Documentation

### Authentication

All endpoints require JWT Bearer token with appropriate scopes:
- `jobs:read` - View job status
- `jobs:write` - Submit jobs

The `sub` claim is the numeric user id. It is the only source of the billed account, so a request
body cannot charge another user, and a job is visible only to the user who submitted it. A token
whose `sub` is not numeric is rejected with 403.

**Token Generation** (development only):

Visit [jwt.io](https://jwt.io) and create a token with:
- Algorithm: `HS256`
- Secret: `dev-secret`
- Payload:
  ```json
  {
    "sub": "1",
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
Idempotency-Key: {unique-key}  # Optional, reuse the same key when retrying. Scoped per user.

{
  "agentSpec": "{\"image\":\"...\"}",  # Required, JSON object as a string
  "resourceHint": "{\"region\":\"us-east-1\",\"gpuType\":\"A100-80G\"}",  # Optional, JSON object as a string
  "maxBudget": 100.0  # Optional, omit for no cap
}

Response: 200 OK
{
  "jobId": 123,
  "status": "RUNNING"
}
```

`agentSpec` and `resourceHint` are stored in MySQL `json` columns, so anything that is not a JSON
object is rejected with 400. `region` and `gpuType` from `resourceHint` drive the quote lookup and
fall back to `us-east-1` / `A100-80G` when absent.

Submission is synchronous: provisioning and start finish before the response is written, so the
status in the response is already `RUNNING`.

`maxBudget` caps the hold placed at submit time, and a job whose hold would exceed it is rejected
with 422 instead of being provisioned. The hold is one hour of the selected provider's rate plus 20
percent, because nothing in the platform yet knows how long a job will run. Runtime is not metered,
so `maxBudget` is not a cap on what a long job ultimately costs.

Without an `Idempotency-Key` a retried submit creates a second job and a second hold. Retries must
send the key of the original attempt.

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

`POST /v1/jobs` walks this whole path inside one transaction, so `QUEUED` and `PROVISIONING` are
never observable through the API today. Only `RUNNING` (or an error) is ever returned by submit.

## Testing

The suite is slice and unit tests, so it runs without Docker.

```bash
# Everything
./gradlew test

# API slice, orchestrator units, and a context load on in-memory H2
./gradlew :app:test

# SDK against MockRestServiceServer
./gradlew :agent-sdk:test
```

Not yet covered: nothing exercises real MySQL, so the `json` columns and the Flyway migrations are
only verified by reading them. `adapters` has no tests.

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
