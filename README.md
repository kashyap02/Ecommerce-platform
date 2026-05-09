# E-Commerce Microservices Platform

A production-grade, event-driven e-commerce backend built with Java 17, Spring Boot 3, Apache Kafka, Redis, and MySQL — fully containerised with Docker Compose.

---

## Architecture Overview

```
                        ┌─────────────────────┐
                        │     API Gateway      │
                        │      :8080           │
                        │  (JWT Auth + Routing)│
                        └────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────▼───────┐ ┌───────▼────────┐ ┌───────▼────────┐
     │  User Service  │ │Product Service │ │ Order Service  │
     │    :8081       │ │    :8082       │ │    :8083       │
     │  (Auth + JWT)  │ │ (Optimistic    │ │  (Publishes    │
     │                │ │  Locking)      │ │  order-placed) │
     └────────┬───────┘ └───────┬────────┘ └───────┬────────┘
              │                  │                  │
              │           ┌──────▼──────────────────▼──────┐
              │           │           Apache Kafka          │
              │           │  Topics:                        │
              │           │  • order-placed                 │
              │           │  • payment-confirmed            │
              │           │  • payment-failed               │
              │           └──────┬──────────────────┬───────┘
              │                  │                  │
     ┌────────▼───────┐ ┌───────▼────────┐ ┌───────▼────────┐
     │    MySQL       │ │Payment Service │ │Notification    │
     │  userdb        │ │    :8084       │ │Service  :8086  │
     │  productdb     │ │ (Consumes      │ │(Consumes       │
     │  orderdb       │ │  order-placed) │ │ payment events)│
     └────────────────┘ └───────┬────────┘ └────────────────┘
                                │
                    ┌───────────▼──────────┐
                    │  Rate Limiter :8085   │
                    │       Redis          │
                    └──────────────────────┘
```

### Event Flow

```
User → API Gateway → Order Service → [order-placed] → Payment Service
                                                              │
                                              ┌───────────────┴───────────────┐
                                              │                               │
                                    [payment-confirmed]              [payment-failed]
                                              │                               │
                                    Order Service (update)         Order Service (update)
                                    Notification Service           Notification Service
```

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| API Gateway | 8080 | Request routing, JWT validation, rate limiting |
| User Service | 8081 | Registration, login, JWT issuance |
| Product Service | 8082 | Product CRUD with optimistic locking |
| Order Service | 8083 | Order creation, Kafka producer & consumer |
| Payment Service | 8084 | Processes orders, publishes payment events |
| Rate Limiter Service | 8085 | Token-bucket rate limiting via Redis |
| Notification Service | 8086 | Consumes payment events, sends notifications |

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3, Spring Cloud Gateway |
| Messaging | Apache Kafka |
| Cache | Redis |
| Database | MySQL 8 |
| Security | JWT (jjwt), Spring Security |
| Build | Maven (multi-module) |
| Containerisation | Docker, Docker Compose |

---

## Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) or [OrbStack](https://orbstack.dev/)
- Git

### Run the Project

```bash
# Clone the repository
git clone https://github.com/kashyap02/ecommerce-platform.git
cd ecommerce-platform

# Start all services
docker compose up --build
```

That's it. Docker Compose will:
1. Start Zookeeper, Kafka, MySQL, and Redis with health checks
2. Create all databases automatically (`userdb`, `productdb`, `orderdb`)
3. Build and start all 7 microservices in the correct order

First build takes ~5–10 minutes (Maven downloads dependencies). Subsequent builds use cache and take ~30 seconds.

### Stop the Project

```bash
# Stop all containers
docker compose down

# Stop and remove database data (fresh start)
docker compose down -v
```

---

## API Endpoints

All requests go through the **API Gateway at `http://localhost:8080`**.

### Auth (Public)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT token |

### Users (Protected — requires `Authorization: Bearer <token>`)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/profile` | Get current user profile |

### Products (Protected)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | List all products |
| GET | `/api/products/{id}` | Get product by ID |
| POST | `/api/products` | Create a product |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product |

### Orders (Protected)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/orders` | Place an order (triggers Kafka event) |
| GET | `/api/orders/{id}` | Get order by ID |
| GET | `/api/orders` | List all orders for current user |

---

## Design Patterns & Highlights

- **Database per Service** — Each service owns its own MySQL database (`userdb`, `productdb`, `orderdb`), ensuring loose coupling
- **Saga Pattern (Choreography)** — Order → Payment → Notification flow is coordinated via Kafka events without a central orchestrator
- **Optimistic Locking** — Product service uses `@Version` to prevent concurrent update conflicts
- **JWT Stateless Auth** — Tokens issued by User Service are validated at the API Gateway level; downstream services skip DB lookups
- **Rate Limiting** — Redis-backed token bucket algorithm in Rate Limiter Service
- **Two-listener Kafka setup** — `PLAINTEXT` for inter-container traffic (port 29092), `PLAINTEXT_HOST` for host machine access (port 9092)

---

## Project Structure

```
ecommerce-platform/
├── api-gateway/
├── user-service/
├── product-service/
├── order-service/
├── payment-service/
├── notification-service/
├── rate-limiter-service/
├── docker-compose.yml
├── mysql-init.sql
└── pom.xml              # Parent POM (multi-module)
```

---

## Environment Notes

The `application.yml` files use Docker service names (`mysql`, `kafka`, `redis`) as hostnames, which only resolve inside the Docker network. The project is designed to be run via `docker compose` — not standalone from an IDE.

---

## Author

**Avnish** — Senior Software Developer  
[GitHub](https://github.com/kashyap02)