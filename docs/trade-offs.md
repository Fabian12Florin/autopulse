# Trade-offs

## 1. Why Microservices Here

The AutoPulse domain has clear sub-domains (users, parcels, routing, fleet, delivery, geography, notifications), so separation by bounded contexts is natural.

## 2. Achieved Benefits

1. **Independent deployment**
   `user-service` and `parcel-service` can be versioned/deployed separately.

2. **Targeted scaling**
   Services with higher load (for example routing) can scale independently.

3. **Fault isolation**
   A bug in one context no longer requires restarting the entire backend.

4. **Clear domain ownership**
   Teams can work on separate services with explicit interfaces.

## 3. Introduced Costs

1. **Operational complexity**
   Gateway, discovery, external auth, messaging, and monitoring are required.

2. **Distributed consistency**
   Cross-service transactions are harder to coordinate.

3. **Integration and testing cost**
   End-to-end tests and contract tests become more important.

4. **Harder config management**
   More environment variables, secrets, and runtime profiles.

## 4. Trade-off Table

| Decision | Benefit | Cost | Minimal mitigation |
|---|---|---|---|
| Split users/parcels into separate services | Independent deployment, clear ownership | Remote calls instead of local calls | Stable API contracts + timeout/retry |
| DB per service | Reduced data coupling | Multiple migrations, more complex observability | Standard provisioning scripts |
| API Gateway + Discovery | Central routing, dynamic service lookup | Extra components to operate | Health checks + observability |
| Kafka for notifications | Temporal decoupling | Broker operations + async debugging | DLQ/retry policy (TODO) |


## 5. Conclusion

For a cloud-native introduction project, the modularity and independent deployment benefits justify the split, even with additional operational cost.
