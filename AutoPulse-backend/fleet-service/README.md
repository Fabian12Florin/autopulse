# Fleet Service

Fleet Service manages the fleet domain of the AutoPulse platform: depots, vehicles, vehicle assignments, vehicle documents, and maintenance records.

---

## Tech Stack

- Java 21
- Spring Boot
- PostgreSQL
- Flyway
- Eureka Client
- Swagger / OpenAPI
- JaCoCo
- Maven

---

## Configuration

Default port:

```yaml
server:
  port: ${PORT:8083}
```

---

## Database

Fleet Service uses PostgreSQL and Flyway migrations.

Schema:

```text
fleet_service
```

Migration files are located in:

```text
src/main/resources/db/migration
```

Hibernate is configured with `ddl-auto: validate`, so schema changes must be handled through Flyway migrations.

---

## Service Discovery

Fleet Service depends on Discovery Service / Eureka.

Default Eureka URL:

```text
http://localhost:8761/eureka
```

Start Discovery Service before starting Fleet Service.

---

## Swagger

Swagger UI:

```text
http://localhost:8083/swagger-ui.html
```

OpenAPI docs:

```text
http://localhost:8083/v3/api-docs
```

For pageable endpoints, controllers should use:

```java
@ParameterObject @PageableDefault(page = 0, size = 10) Pageable pageable;
```

This makes Swagger display `page`, `size`, and `sort` correctly.

---

## Main Endpoints

| Area        | Base path                                             |
|-------------|-------------------------------------------------------|
| Depots      | `/api/fleet/depots`                                   |
| Vehicles    | `/api/fleet/vehicles`                                 |
| Assignments | `/api/fleet/vehicle-assignments`                      |
| Documents   | `/api/fleet/vehicles/{vehicleId}/documents`           |
| Maintenance | `/api/fleet/vehicles/{vehicleId}/maintenance-records` |

---

## Run Locally

Start Discovery Service:

```bash
mvn -pl discovery-service spring-boot:run
```

Start Fleet Service:

```bash
mvn -pl fleet-service spring-boot:run
```

---

## Tests and Coverage

Run tests:

```bash
mvn -pl fleet-service test
```

Generate JaCoCo coverage:

```bash
mvn -pl fleet-service clean test jacoco:report
```

Coverage report:

```text
fleet-service/target/site/jacoco/index.html
```

