# Payment Engine

A project for building a resilient, event-driven payment processing system with Java, Spring Boot and AWS

> This project processes simulated transactions only. It does not handle real money or customer data.


## Overview
Payment-engine demonstrates production-oriented backend patterns commonly used in payment and transaction-processing system:
- Asynchronous payment processing
- Idempotent request handling
- Transactional outbox
- Retry and dead-latter queue handling
- Explicit payment state transitions
- Persistent PostgreSQL storage
- Correlation IDs, structured logging, and custom metrics
- Automated unit, integration, and end-to-end testing

A payment is initially stored as CREATED. A transactional outbox event is then published to an SQS-compatible queue and processed asynchronously until the payment reaches SUCCEEDED or FAILED.


## Architecture

    flowchart LR
        Client["Client"] --> API["Payment REST API"]
    
        API --> DB["PostgreSQL Transaction"]
        DB --> Payments["Payments"]
        DB --> Idempotency["Idempotency Records"]
        DB --> Outbox["Outbox Events"]
    
        Outbox --> Publisher["Outbox Publisher"]
        Publisher --> Queue["SQS-Compatible Queue"]
        Queue --> Consumer["Payment Event Consumer"]
        Consumer --> Processor["Payment Processor"]
        Processor --> Payments
    
        Queue -.->|After maximum retries| DLQ["Dead-Letter Queue"]

## Payment Lifecycle
    stateDiagram-v2
    [*] --> CREATED
    CREATED --> PROCESSING
    PROCESSING --> SUCCEEDED
    PROCESSING --> FAILED

Invalid transitions are rejected. For example, a SUCCEEDED payment cannot be processed again or changed to FAILED.

## Reliability Design

### Idempotency

Each payment request requires an Idempotency-Key header.

- Repeating the same key with the same payment details returns the existing payment.

- Reusing the key with different payment details returns 409 Conflict.

- Database persistence prevents duplicate payment creation.

### Transactional Outbox

Payment and outbox records are stored in the same PostgreSQL transaction. This avoids the dual-write problem where the database update succeeds but message publication fails.

The outbox publisher later sends pending events to the queue and marks them as PUBLISHED.

### Retry and Dead-Letter Queue

Messages are deleted only after successful processing.

- Failed messages remain in the queue for retry.
- The consumer tracks the approximate receive count.
- After the maximum number of attempts, processing failures are marked FAILED.
- Messages that continue to fail are moved to the dead-letter queue.

### Idempotent Consumer

A successfully processed payment is not processed again. This protects the system from duplicate message delivery.

## Tech Stack

- Backend: Java 17, Spring Boot 4.0.7, Spring Web MVC, Jakarta Bean Validation
- Persistence: Spring Data JPA, PostgreSQL 17, Flyway
- Messaging: AWS SDK for Java v2 (SQS), ElasticMQ 1.7.1
- Observability: Spring Boot Actuator, Micrometer
- Testing: JUnit 5, AssertJ, Mockito, Testcontainers
- Build and DevOps: Maven, Docker Compose, GitHub Actions

## Running Locally

### Prerequisites
- Java 17
- Docker Desktop

### 1. Start PostgreSQL and ElasticMQ

    docker compose up -d

Confirm that both services are running:

    docker compose ps

### 2. Start the application

    OUTBOX_PUBLISHER_ENABLED=true \
    SQS_CONSUMER_ENABLED=true \
    ./mvnw spring-boot:run

The application starts at:

    http://localhost:8080

### 3. Check application health

    curl http://localhost:8080/actuator/health

Expected response:

    {
    "groups": ["liveness", "readiness"],
    "status": "UP"
    }

### API Examples

Create a Payment

    curl -i -X POST http://localhost:8080/payments \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: checkout-request-001" \
    -H "X-Correlation-ID: payment-request-001" \
    -d '{"amount":42.50,"currency":"USD"}'

Example response:

    {
    "id": "d2c1973e-3507-4982-ad14-59d250eaaa63",
    "amount": 42.50,
    "currency": "USD",
    "status": "CREATED"
    }

The payment is processed asynchronously after creation.

### Retrieve a Payment

    curl http://localhost:8080/payments/d2c1973e-3507-4982-ad14-59d250eaaa63

Example final response:

    {
    "id": "d2c1973e-3507-4982-ad14-59d250eaaa63",
    "amount": 42.50,
    "currency": "USD",
    "status": "SUCCEEDED"
    }

### Validation Error

    curl -i -X POST http://localhost:8080/payments \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: invalid-request-001" \
    -d '{"amount":10.99,"currency":"usd"}'

Example response:

    {
    "code": "INVALID_REQUEST",
    "message": "currency must be a 3-letter uppercase code"
    }

### Observability

#### Correlation IDs

Clients may provide an X-Correlation-ID request header. If it is missing or invalid, the application generates one.

The correlation ID is:

- Returned in the response header
- Added to request-completion logs
- Useful for tracing one request across application logs

#### Metrics

Payment-processing outcomes are recorded with the following tags:
- outcome=success
- outcome=retry
- outcome=final_failure

View the success counter:

    curl \
    "http://localhost:8080/actuator/metrics/payment.processing?tag=outcome:success"

Actuator metrics are intended for local inspection. A production deployment would export metrics to an external monitoring backend.

### Testing

Run the complete test suite:

    ./mvnw test

The project currently includes 66 automated tests covering:

- Domain and state-transition rules
- Money validation
- API request validation
- Idempotency behavior and conflicts
- Repository persistence
- PostgreSQL integration
- Transaction rollback
- Transactional outbox behavior
- SQS publishing and consumption
- Retry and final-failure behavior
- Correlation IDs
- Custom metrics
- End-to-end payment processing
- Duplicate-request E2E behavior

PostgreSQL and ElasticMQ integration tests use Testcontainers, so Docker Desktop must be running.

### Continuous Integration

GitHub Actions runs the complete Maven test suite for pushes and pull requests.

### Project Structure

    src/main/java/dev/portfolio/payment
    ├── api
    │   ├── REST controllers
    │   ├── request and response models
    │   ├── validation
    │   └── exception and correlation handling
    ├── application
    │   ├── payment use cases
    │   ├── payment processing
    │   └── outbox event creation
    ├── domain
    │   ├── payment lifecycle
    │   ├── money
    │   ├── idempotency
    │   └── repository interfaces
    └── infrastructure
    ├── PostgreSQL persistence
    ├── SQS-compatible messaging
    └── observability

### Local Infrastructure

Docker Compose provides:

- PostgreSQL for persistent application data
- ElasticMQ as a local Amazon SQS-compatible queue
- A payment-processing queue
- A payment-processing dead-letter queue

The application uses the AWS SDK for Java, allowing the messaging implementation to target AWS SQS by changing environment-specific configuration.

### Future Improvements

- Deploy the application and messaging infrastructure to AWS
- Add OpenTelemetry distributed tracing
- Add authentication and authorization
- Add refund lifecycle support
- Add an external payment-provider adapter
- Export metrics to a production monitoring platform
