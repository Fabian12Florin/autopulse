# Decomposition Strategy

## 1. Objective

To demonstrate the transformation of a monolithic application into a cloud-native microservices set, with separation by bounded contexts and independent deployment.

## 2. Initial Monolith (Conceptual Model)

Initial assumption:

- one single Spring Boot backend
- one single deployment pipeline
- internal modules for users, parcels, fleet, routing, delivery, geography, notifications
- high coupling between modules through direct calls and shared models

Monolith issues:

- synchronized release for any change
- global scaling (not per context)
- larger blast radius in case of defects

## 3. Criteria Used for the Split

1. **Business capability**
   Separation is done by business responsibility.

2. **Clear ownership**
   Each context owns its endpoints, rules, and data.

3. **Deployability**
   Each context becomes a separate artifact/container.

4. **Explicit contracts**
   Interaction between services happens through APIs or events.

## 4. Extracted Bounded Contexts

- User & Identity -> `user-service`
- Parcel Lifecycle -> `parcel-service`
- Fleet -> `fleet-service`
- Routing -> `routing-service`
- Delivery Execution -> `delivery-execution-service`
- Geography -> `geography-service`
- Notifications -> `notification-service`

## 5. The Two Minimum Contexts Required by the Assignment

### A. User & Identity

In the monolith:

- user management and auth modules in the same application
- direct calls to methods/repositories from other modules

After the split:

- `user-service` fully isolates the user domain
- auth integrated with Keycloak
- user data consumed through service-to-service contracts

### B. Parcel Lifecycle

In the monolith:

- parcel create/status logic mixed with routing/delivery
- concurrent access to the same tables by multiple modules

After the split:

- `parcel-service` becomes the owner of parcel lifecycle
- routing/delivery consume parcel data through APIs

## 6. Practical Refactoring Steps (Documented)

1. identify aggregates and business rules per context
2. extract endpoints per context
3. separate persistence (DB/schema ownership)
4. introduce gateway and discovery
5. integrate external auth
6. add baseline observability

## 7. Technical Evidence in This Project

- separate Maven modules in `AutoPulse-backend/pom.xml`
- separate `application*` files per service
- separate `Dockerfile` per service
- separate deployment descriptors (`docker-compose.yml`, `k8s/*.yaml`)

## 8. Data Decomposition

Script `AutoPulse-backend/infra/postgres/sql/01-create-microservice-database.sql` creates separate databases per context, reducing shared-database coupling.

## 9. Conclusion

The decomposition is valid for the "Cloud native introduction" assignment: bounded context separation exists, independent deployment is supported, and trade-offs are explicitly documented.
