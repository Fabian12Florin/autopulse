# AutoPulse - Monolith to Microservices Refactor (Cloud Native Intro)

## 1. Assignment Goal

The lab requirement is:

- start from a monolithic application model
- split at least 2 bounded contexts into independent services
- demonstrate independent deployment
- document the strategy and trade-offs

This repository already contains a microservices implementation. In this documentation, the project is presented explicitly as the outcome of a refactor from a conceptual monolith.

## 2. Project Overview

- Frontend: `AutoPulse-frontend` (React + TypeScript)
- Backend: `AutoPulse-backend` (Spring Boot, multi-module)
- Domain: parcel delivery operations (users, fleet, parcels, routing, delivery execution, geography, notifications)

## 3. Monolith Structure (Before Refactoring)

In a monolithic version, the system would have been a single deployable (single process) with internal modules:

- `users-auth`
- `parcels`
- `fleet`
- `routing`
- `delivery-execution`
- `geography`
- `notifications`

Typical monolith characteristics:

- one build artifact
- one release unit
- in-process calls between modules
- risk of coupled changes

## 4. Existing Microservices in This Project

Current backend services:

- `discovery-service`
- `api-gateway`
- `user-service`
- `fleet-service`
- `parcel-service`
- `routing-service`
- `delivery-execution-service`
- `geography-service`
- `notification-service`

Supporting components:

- Keycloak (identity provider)
- Kafka (event streaming)
- Postgres
- Prometheus + Grafana
- VROOM + OSRM (routing engine)

## 5. Bounded Contexts (Minimum Requirement: At Least 2)

### Context 1: User & Identity -> `user-service`

Responsibilities:

- users, couriers, dispatchers
- roles and operational profiles
- auth integration with Keycloak

### Context 2: Parcel Lifecycle -> `parcel-service`

Responsibilities:

- parcel creation
- parcel lifecycle status updates
- data required for routing and delivery execution

Additional separated contexts:

- Fleet -> `fleet-service`
- Routing -> `routing-service`
- Delivery Execution -> `delivery-execution-service`
- Geography -> `geography-service`
- Notifications -> `notification-service`

## 6. How the Decomposition Was Done (Monolith -> Microservices)

Applied strategy:

1. split by business capabilities (not by technical layers)
2. separate ownership of model + API + persistence by context
3. replace in-process calls with HTTP/event communication
4. introduce cloud-native infrastructure (discovery, gateway, observability)

Details: [docs/decomposition-strategy.md](D:\autopulse-both\docs\decomposition-strategy.md)

## 7. Evidence of Independent Deployment

For each business service there is:

- separate build (`<service>/pom.xml`)
- separate config (`application.yml|yaml|properties`)
- separate container image (`<service>/Dockerfile`)
- separate port (`server.port` + env vars)

Example build/deploy commands per service:

```bash
cd AutoPulse-backend
mvn -pl user-service -am clean package
mvn -pl parcel-service -am clean package
```

Architecture details and independence matrix: [docs/architecture.md](D:\autopulse-both\docs\architecture.md)

## 8. Config/Port/Build/Database Verification

- Separate config: present in each service
- Separate port: present in each service
- Separate build: present in each service
- Data isolation:
  - SQL script for separate databases: `AutoPulse-backend/infra/postgres/sql/01-create-microservice-database.sql`
  - Dedicated DBs: `user_db`, `parcel_db`, `fleet_db`, `routing_db`, `delivery_db`, `geography_db`, `notification_db`, `keycloak_db`

## 9. Local Run

### 9.1 Run Full Backend (Compose)

```bash
cd AutoPulse-backend
copy .env.example .env
```

```bash
docker network create autopulse-network
cd infra/postgres
docker compose up -d
cd ../../
docker compose up -d --build
```

### 9.2 Run Frontend

```bash
cd AutoPulse-frontend
npm install
npm run dev
```

- Frontend: `http://localhost:5173`
- Gateway: `http://localhost:8080`

### 9.3 Run a Single Service (Example)

```bash
cd AutoPulse-backend
mvn -pl parcel-service -am spring-boot:run
```

## 10. Trade-offs (Summary)

Benefits:

- independent deployment per bounded context
- independent scaling
- better fault isolation

Costs:

- higher operational complexity
- harder distributed consistency
- more configuration variables to manage

Details: [docs/trade-offs.md](D:\autopulse-both\docs\trade-offs.md)
