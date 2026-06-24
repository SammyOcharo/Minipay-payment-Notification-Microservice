# MiniPay — Payment & Notification Microservice

A production-like payment microservice system built with Java Spring Boot,
featuring real M-PESA Daraja integration, event-driven notifications via
Kafka, and a secure API Gateway.

---

## Architecture

Client

│

▼

API Gateway (Port 8080)        ← JWT Auth + OAuth2 + Routing

│

├──▶ Payment Service (8081)  ← M-PESA Daraja + Card + Idempotency

│         │

│         └──▶ Kafka (payment.events topic)

│                   │

└──▶ Notification Service (8082) ← SMS via Africa's Talking


---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| API Gateway | Spring Cloud Gateway (Reactive) |
| Security | JWT + Google OAuth2 |
| Messaging | Apache Kafka |
| Database | PostgreSQL (per service) |
| Payment | M-PESA Daraja STK Push |
| SMS | Africa's Talking |
| Resilience | Resilience4j (Circuit Breaker + Retry) |
| Docs | SpringDoc OpenAPI / Swagger |
| Containers | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---
## API Documentation

Swagger UI: http://34.246.208.69:8080/webjars/swagger-ui/index.html
API Docs JSON: http://34.246.208.69:8080/api-docs

## Services

### API Gateway (Port 8080)
- JWT token generation and validation
- Google OAuth2 login
- Request routing to downstream services
- Correlation ID logging

### Payment Service (Port 8081)
- Payment initiation (M-PESA STK Push + Card)
- Idempotency — prevents double charges
- Circuit Breaker + Retry via Resilience4j
- Kafka event publishing
- Swagger UI at `/swagger-ui.html`

### Notification Service (Port 8082)
- Kafka event consumption
- SMS notifications via Africa's Talking
- Deduplication — prevents duplicate SMS

---

## Prerequisites

- Java 17+
- Docker + Docker Compose
- Maven 3.9+
- ngrok (for M-PESA callbacks)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/your-username/minipay.git
cd minipay
```

### 2. Set up environment variables

```bash
cp .env.example .env
```

Edit `.env` with your real credentials:

```env
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
MPESA_CONSUMER_KEY=your-mpesa-consumer-key
MPESA_CONSUMER_SECRET=your-mpesa-consumer-secret
MPESA_PASSKEY=your-mpesa-passkey
MPESA_CALLBACK_URL=https://your-ngrok-url/api/payments/mpesa/callback
AT_USERNAME=sandbox
AT_API_KEY=your-at-api-key
JWT_SECRET=your-jwt-secret-min-256-bits
```

### 3. Start ngrok for M-PESA callbacks

```bash
ngrok http 8081
```

Update `MPESA_CALLBACK_URL` in `.env` with the ngrok URL.

### 4. Start all services

```bash
# Start infrastructure
docker-compose up -d postgres-gateway postgres-payments \
  postgres-notifications zookeeper kafka kafka-ui

# Start microservices
docker-compose up -d api-gateway payment-service notification-service
```

### 5. Verify all services are running

```bash
docker-compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

---

## API Usage

### Register

```bash
curl -X POST http://34.246.208.69:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "John Doe",
    "email": "john@minipay.com",
    "password": "password123",
    "phoneNumber": "+254712345678"
  }'
```

### Login

```bash
curl -X POST http://34.246.208.69:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@minipay.com",
    "password": "password123"
  }'
```

### Initiate Payment (M-PESA STK Push)

```bash
curl -X POST http://34.246.208.69:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "amount": 100.00,
    "currency": "KES",
    "phoneNumber": "+254712345678",
    "paymentMethod": "MPESA",
    "description": "Test payment"
  }'
```

### Get Payment

```bash
curl http://34.246.208.69:8080/api/payments/{id} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Payment History

```bash
curl http://34.246.208.69:8080/api/payments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Key Features

### Idempotency
Every payment request requires an `Idempotency-Key` header.
Duplicate requests with the same key return the cached response
without processing a new payment — preventing double charges.

### Circuit Breaker
Resilience4j circuit breakers protect M-PESA and Card gateway calls.
After 50% failure rate, the circuit opens and returns a fallback response.

### Event-Driven SMS
Payment events are published to Kafka `payment.events` topic.
The Notification Service consumes events and sends SMS via Africa's Talking.

---

## Monitoring

| Service | URL |
|---|---|
| Kafka UI | http://localhost:8090 |
| Payment Swagger | http://34.246.208.69:8080/swagger-ui.html |
| Gateway Health | http://34.246.208.69:8080/actuator/health |

---

## Running Tests

```bash
# All services
cd api-gateway && ./mvnw test && cd ..
cd payment-service && ./mvnw test && cd ..
cd notification-service && ./mvnw test && cd ..
```

**Test Coverage:**
- API Gateway: 12 tests
- Payment Service: 8 tests
- Notification Service: 5 tests

---

## Project Structure

minipay/

├── api-gateway/              # Spring Cloud Gateway + Auth

├── payment-service/          # Payment processing + Daraja

├── notification-service/     # Kafka consumer + SMS

├── docker-compose.yml        # Full stack orchestration

├── .env.example              # Environment template

├── .github/

│   └── workflows/

│       └── ci.yml            # GitHub Actions pipeline

└── README.md


---

## CI/CD Pipeline

GitHub Actions pipeline runs on every push to `main` or `develop`:

1. Run tests for all 3 services in parallel
2. Build JAR artifacts
3. Build and push Docker images to Docker Hub (main branch only)

---

## Design Decisions

- **Database per service** — each microservice owns its data
- **API Gateway as single entry point** — all auth handled centrally
- **Kafka for async notifications** — payment service doesn't wait for SMS
- **Idempotency at service level** — safe retries, no double charges
- **Strategy pattern for gateways** — easily swap M-PESA/Card implementations
- **Circuit Breaker per gateway** — isolated failures, graceful degradation

---

## Author

Built by **Sammy Ocharo Obanyi** as a backend engineering assessment.
