# Parcel Delivery Microservices Platform

A scalable backend system for managing parcel delivery operations, including routing optimization, fleet management, delivery execution, and notifications.

---

##  Overview

This project is a **microservices-based architecture** designed for a delivery company. It handles the full lifecycle of parcels:

- creation and management
- route optimization
- courier assignment
- delivery execution
- real-time tracking
- notifications

---

##  Tech Stack

- Java 21
- Spring Boot
- Spring Security + Keycloak
- PostgreSQL
- Maven
- Docker / Docker Compose
- Kafka

---

##  Authentication & Authorization

- Managed via **Keycloak**
- JWT-based authentication
- Roles:
   - ADMIN
   - DISPATCHER
   - COURIER

---

##  Core Domain Concepts

### Parcel
- Represents a delivery item

### Courier
- User responsible for deliveries

### Vehicle
- Managed in fleet-service
- Capacity constraints (weight, volume)

### Route
- Computed by routing-service
- Contains ordered stops

### Delivery Execution
- Tracks real-world delivery progress
- Includes:
   - scan events
   - delivery attempts
   - proof of delivery (PIN)