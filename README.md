# Strata — Event Sourcing Demo

A production-style demo showcasing **Event Sourcing + CQRS** with Kotlin/Spring Boot, Kafka, Debezium CDC, PostgreSQL, MySQL, and Kubernetes.

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
```

### How It Works

1. **Command** → REST API → Command Service → Validates via Aggregate → Saves event to PostgreSQL
2. **CDC** → Debezium monitors PostgreSQL WAL → Publishes change to Kafka topic `postgres.public.events`
3. **Projection** → Query Service consumes Kafka topic → Updates MySQL read model
4. **Query** → REST API → Query Service → Reads from MySQL

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

This starts: PostgreSQL, MySQL, Kafka (KRaft), Debezium Connect, Command Service, Query Service, and Kafka UI.

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
    "initialBalance": 1000.00
  }' | jq
```

**Deposit money:**
```bash
curl -s -X POST http://localhost:8080/api/accounts/{ACCOUNT_ID}/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500.00,
    "description": "Monthly salary"
  }' | jq
```

**Withdraw money:**
```bash
curl -s -X POST http://localhost:8080/api/accounts/{ACCOUNT_ID}/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 200.00,
    "description": "Groceries"
  }' | jq
```

**Query account (from read model):**
```bash
curl -s http://localhost:8081/api/accounts | jq
curl -s http://localhost:8081/api/accounts/{ACCOUNT_ID} | jq
```

### 5. Monitor

- **Kafka UI**: http://localhost:9090
- **Debezium Connect API**: http://localhost:8083/connectors/

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

# Deploy application services
kubectl apply -f k8s/command/
kubectl apply -f k8s/query/

# Deploy ingress
kubectl apply -f k8s/ingress.yaml
```

## Project Structure

```
Strata/
├── shared/          # Shared kernel (events, DTOs)
├── command/         # Write-side microservice (PostgreSQL)
├── query/           # Read-side microservice (MySQL + Kafka)
├── docker/          # Docker Compose & Dockerfiles
└── k8s/             # Kubernetes manifests
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
| DB Migration      | Flyway                              |
| Build Tool        | Gradle 8.12 (Kotlin DSL)            |
| Runtime           | JDK 21                              |
| Container Runtime | Docker / Kubernetes                 |

## Key Design Decisions

- **Event Store in PostgreSQL**: Uses JSONB for flexible event data, composite unique constraint on `(aggregate_id, version)` for optimistic concurrency.
- **CDC over dual-write**: Debezium monitors the PostgreSQL WAL, eliminating the need for the command service to produce Kafka messages directly.
- **Idempotent projections**: The query service tracks `event_version` per aggregate to skip already-processed events.
- **KRaft mode**: Kafka runs without Zookeeper for a simpler deployment topology.
