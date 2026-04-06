# Strata — Event Sourcing Demo

A production-style demo showcasing **Event Sourcing + CQRS** with Kotlin/Spring Boot, Kafka, Debezium CDC, PostgreSQL, MySQL, Apache Camel K, and Kubernetes.

## Architecture

```
┌─────────────┐    HTTP     ┌──────────────────┐     JPA      ┌─────────────┐
│   Client     │───────────▶│  Command Service  │────────────▶│  PostgreSQL  │
│              │            │  (port 8080)      │             │  (Event Store)│
└─────────────┘            └──────────────────┘             └──────┬────────┘
                                                                    │ WAL
                                                            ┌──────▼────────┐
                                                            │   Debezium     │
                                                            │  (Kafka Connect│
                                                            │   CDC)         │
                                                            └──────┬────────┘
                                                                    │
                                                            ┌──────▼────────┐
┌─────────────┐    HTTP     ┌──────────────────┐  Kafka     │    Kafka       │
│   Client     │───────────▶│  Query Service    │◀──────────│    (KRaft)     │
│              │            │  (port 8081)      │           └───────────────┘
└─────────────┘            └────────┬─────────┘
                                     │ JPA
                              ┌──────▼────────┐
                              │    MySQL       │
                              │  (Read Model)  │
                              └───────────────┘

┌─────────────┐    HTTP     ┌──────────────────┐  HTTP      ┌──────────────────┐
│   Client     │───────────▶│  Camel Router    │───────────▶│  Fee Service US  │ (port 8091)
│              │            │  (port 8082)      │           ├──────────────────┤
└─────────────┘            └────────┬─────────┘           │  Fee Service EU  │ (port 8092)
                                     │                     ├──────────────────┤
                                     │ Content-Based       │  Fee Service VN  │ (port 8093)
                                     │ Routing by country  └──────────────────┘
```

### How It Works

1. **Command** → REST API → Command Service → Validates via Aggregate → Saves event to PostgreSQL
2. **CDC** → Debezium monitors PostgreSQL WAL → Publishes change to Kafka topic `postgres.public.events`
3. **Projection** → Query Service consumes Kafka topic → Updates MySQL read model
4. **Query** → REST API → Query Service → Reads from MySQL
5. **Smart Routing** → Camel Router receives a fee calculation request → Routes to the specialized fee service (US, EU, or VN pod) based on the `country` field → Returns the calculated fee + total to the client.

> **No dual-write problem**: The command service only writes to PostgreSQL. Debezium handles replication to Kafka automatically via CDC.

## Quick Start (Docker Compose)

### Prerequisites
- Docker & Docker Compose
- JDK 21
- Gradle 8.12+

### 1. Build the Application

```bash
./gradlew clean build -x test
```

### 2. Start All Services

```bash
cd docker
docker compose up -d
```

This starts: PostgreSQL, MySQL, Kafka (KRaft), Debezium Connect, Command Service, Query Service, **Smart Router**, and Kafka UI.

### 3. Register the Debezium Connector

Wait for Kafka Connect to be ready (~30s), then:

```bash
curl -i -X POST \
  -H "Accept:application/json" \
  -H "Content-Type:application/json" \
  http://localhost:8083/connectors/ \
  -d @docker/debezium/register-connector.json
```

### 4. Test the API

**Create an account:**
```bash
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountName": "Alice Savings",
    "initialBalance": 1000
  }' | jq
```

**Deposit money:**
```bash
curl -s -X POST http://localhost:8080/api/accounts/{ACCOUNT_ID}/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500,
    "description": "Monthly salary"
  }' | jq
```

**Withdraw money:**
```bash
curl -s -X POST http://localhost:8080/api/accounts/{ACCOUNT_ID}/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 200,
    "description": "Groceries"
  }' | jq
```

**Query account (from read model):**
```bash
curl -s http://localhost:8081/api/accounts | jq
curl -s http://localhost:8081/api/accounts/{ACCOUNT_ID} | jq
```

### 5. Test Smart Routing (Fraud Detection)

Deposit a large amount to trigger the fraud detector:

```bash
# Deposit above the 50,000 threshold → triggers HIGH alert
curl -s -X POST http://localhost:8080/api/accounts/{ACCOUNT_ID}/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 75000,
    "description": "Large wire transfer"
  }' | jq

# Deposit above 2× threshold → triggers CRITICAL alert
curl -s -X POST http://localhost:8080/api/accounts/{ACCOUNT_ID}/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 150000,
    "description": "Suspicious international transfer"
  }' | jq
```

Check the router logs for alert output:
```bash
docker logs strata-router --tail 50
```

You should see the console notification:
```
╔══════════════════════════════════════════════════════════╗
║           HIGH-VALUE TRANSACTION ALERT                  ║
╠══════════════════════════════════════════════════════════╣
║  Alert Level  : HIGH
║  Account      : <account-id>
║  Event Type   : MoneyDeposited
║  Amount       : 75000
║  Threshold    : 50000
║  Message      : Transaction of 75000 exceeds threshold of 50000
╚══════════════════════════════════════════════════════════╝
```

### 6. Monitor

- **Kafka UI**: http://localhost:9090 — inspect the new topics:
  - `account-lifecycle`
  - `transaction-processed`
  - `high-value-alerts`
  - `dead-letter-events`
- **Debezium Connect API**: http://localhost:8083/connectors/
- **Router Actuator**: http://localhost:8082/actuator/health

### Smart Routing — Country-Based Fee Calculation

The `router` module demonstrates the **Content-Based Router** Enterprise Integration Pattern (EIP). When a transaction is initiated, the router:
1. Inspects the `country` field in the request.
2. Routes the request to a country-specific fee calculation microservice.
3. Each country microservice (US, EU, VN) runs as a separate deployment/pod and is built from its own independent Kotlin module (`fee-service-us`, `fee-service-eu`, `fee-service-vn`). This demonstrates a shared-nothing microservice architecture.
4. Aggregates the fee and returns the total amount to the client.

| Pattern | Implementation | Description |
|---------|---------------|-------------|
| **Content-Based Router** | `TransferFeeRoute.kt` | Routes requests by `country` to specialized fee services |

### Deployment Options

The smart routing logic is available in two forms:

1. **Spring Boot + Camel** (default) — Runs as a containerized microservice in the `router/` module. Works with Docker Compose and standard Kubernetes Deployments.
2. **Camel K YAML DSL** — Standalone `.camel.yaml` file in the `camelk/` directory. Deploy directly on Kubernetes using the Camel K operator (`kamel run`). See [`camelk/README.md`](camelk/README.md).

## Quick Start (Docker Compose)

### 1. Build the Application
```bash
./gradlew clean build -x test
```

### 2. Start All Services
```bash
cd docker
docker compose up -d
```

### 3. Test Fee Calculation
```bash
# US fee: 0.5% flat rate ($50)
curl -s -X POST http://localhost:8082/api/transfer/calculate-fee \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000, "country": "US"}' | jq

# EU fee: 0.3% rate (€15)
curl -s -X POST http://localhost:8082/api/transfer/calculate-fee \
  -H "Content-Type: application/json" \
  -d '{"amount": 5000, "country": "EU"}' | jq

# VN fee: flat 10,000 VND
curl -s -X POST http://localhost:8082/api/transfer/calculate-fee \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000000, "country": "VN"}' | jq
```

## Kubernetes Deployment

### Prerequisites
- Minikube or Kind
- kubectl

### Deploy

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Deploy infrastructure
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/mysql/
kubectl apply -f k8s/kafka/

# Wait for infrastructure to be ready
kubectl -n strata wait --for=condition=ready pod -l app=postgres --timeout=120s
kubectl -n strata wait --for=condition=ready pod -l app=mysql --timeout=120s
kubectl -n strata wait --for=condition=ready pod -l app=kafka --timeout=120s

# Deploy Debezium
kubectl apply -f k8s/debezium/

# Build and load images (Minikube)
eval $(minikube docker-env)
docker build -t strata-command:latest -f docker/command.Dockerfile .
docker build -t strata-query:latest -f docker/query.Dockerfile .
docker build -t strata-router:latest -f docker/router.Dockerfile .

# Deploy application services
kubectl apply -f k8s/command/
kubectl apply -f k8s/query/
kubectl apply -f k8s/router/

# Deploy ingress
kubectl apply -f k8s/ingress.yaml
```

### Alternative: Deploy with Camel K Operator

If you have the Camel K operator installed, you can deploy the smart routing
logic as native Kubernetes integrations instead of the `router/` module:

```bash
# Install Camel K operator (one-time)
kamel install --namespace strata

# Deploy the smart router as a Camel K Integration
kubectl apply -f k8s/camelk/integration.yaml
```

See [`camelk/README.md`](camelk/README.md) for detailed instructions.

## Project Structure

```
Strata/
├── shared/          # Shared kernel (events, DTOs, routing constants)
├── command/         # Write-side microservice (PostgreSQL)
├── query/           # Read-side microservice (MySQL + Kafka)
├── router/          # Smart Router (Apache Camel — content-based routing)
├── camelk/          # Standalone Camel K YAML routes (for K8s operator)
├── docker/          # Docker Compose & Dockerfiles
└── k8s/             # Kubernetes manifests
    ├── command/
    ├── query/
    ├── router/      # K8s Deployment for the router module
    └── camelk/      # Camel K Integration CRDs
```

## Technology Stack

| Component         | Technology                          |
|-------------------|-------------------------------------|
| Language          | Kotlin 1.9                          |
| Framework         | Spring Boot 3.4                     |
| Write Database    | PostgreSQL 16 (Event Store, JSONB)  |
| Read Database     | MySQL 8.0 (Projections)             |
| Message Broker    | Apache Kafka 3.7 (KRaft)            |
| CDC               | Debezium 2.5 (PostgreSQL Connector) |
| Smart Routing     | Apache Camel 4.4 / Camel K          |
| DB Migration      | Flyway                              |
| Build Tool        | Gradle 8.12 (Kotlin DSL)            |
| Runtime           | JDK 21                              |
| Container Runtime | Docker / Kubernetes                 |

## Key Design Decisions

- **Event Store in PostgreSQL**: Uses JSONB for flexible event data, composite unique constraint on `(aggregate_id, version)` for optimistic concurrency.
- **CDC over dual-write**: Debezium monitors the PostgreSQL WAL, eliminating the need for the command service to produce Kafka messages directly.
- **Idempotent projections**: The query service tracks `event_version` per aggregate to skip already-processed events.
- **KRaft mode**: Kafka runs without Zookeeper for a simpler deployment topology.
- **Smart Routing via Camel**: Content-based routing runs as a separate consumer group alongside the query service, ensuring zero impact on existing event processing. Uses Wire Tap for non-blocking fraud detection.
- **Dead Letter Channel**: Unknown or unparseable events are routed to a dead-letter topic with full error context for later inspection and replay.
- **Dual deployment**: Smart routing logic is available both as a Spring Boot service (traditional deployment) and as standalone Camel K YAML routes (cloud-native, operator-managed deployment).
