# AutoPulse - Refactorizare Monolit -> Microservicii (Cloud Native Intro)

## 1. Scopul temei

Cerinta laboratorului este:

- pornim de la un model de aplicatie monolitica
- separam cel putin 2 bounded contexts in servicii independente
- demonstram deploy independent
- documentam strategia si trade-offs

Acest repository contine deja implementarea microservicii. In aceasta documentatie, proiectul este prezentat explicit ca rezultat al refactorizarii dintr-un monolit conceptual.

## 2. Overview proiect

- Frontend: `AutoPulse-frontend` (React + TypeScript)
- Backend: `AutoPulse-backend` (Spring Boot, multi-module)
- Domeniu: operatiuni de livrare colete (utilizatori, flota, colete, rutare, executie livrare, geografie, notificari)

## 3. Structura monolitului (inainte de refactorizare)

In varianta monolitica, sistemul ar fi fost un singur deployable (un singur proces) cu module interne:

- `users-auth`
- `parcels`
- `fleet`
- `routing`
- `delivery-execution`
- `geography`
- `notifications`

Caracteristici tipice monolit:

- un artifact de build
- release unic
- apeluri in-process intre module
- risc de coupled changes

## 4. Microservicii identificate in proiect

Servicii backend existente:

- `discovery-service`
- `api-gateway`
- `user-service`
- `fleet-service`
- `parcel-service`
- `routing-service`
- `delivery-execution-service`
- `geography-service`
- `notification-service`

Componente suport:

- Keycloak (identity provider)
- Kafka (event streaming)
- Postgres
- Prometheus + Grafana
- VROOM + OSRM (routing engine)

## 5. Bounded Contexts (cerinta minima: cel putin 2)

### Context 1: User & Identity -> `user-service`

Responsabilitati:

- utilizatori, curieri, dispatcheri
- roluri si profile operationale
- integrare auth cu Keycloak

### Context 2: Parcel Lifecycle -> `parcel-service`

Responsabilitati:

- creare colet
- actualizare status lifecycle
- date necesare pentru rutare si executie livrare

Contexturi suplimentare separate:

- Fleet -> `fleet-service`
- Routing -> `routing-service`
- Delivery Execution -> `delivery-execution-service`
- Geography -> `geography-service`
- Notifications -> `notification-service`

## 6. Cum a fost facuta decompozitia (monolit -> microservicii)

Strategia aplicata:

1. delimitare pe business capabilities (nu pe layere tehnice)
2. separare ownership model + API + persistenta pe context
3. inlocuire apeluri in-process cu apeluri HTTP / evenimente
4. introducere infrastructura cloud-native (discovery, gateway, observability)

Detaliere: [docs/decomposition-strategy.md](D:\autopulse-both\docs\decomposition-strategy.md)

## 7. Dovada de deployment independent

Pentru fiecare serviciu de business exista:

- build separat (`<service>/pom.xml`)
- config separat (`application.yml|yaml|properties`)
- imagine/container separat (`<service>/Dockerfile`)
- port separat (`server.port` + variabile env)

Exemple de build/deploy per serviciu:

```bash
cd AutoPulse-backend
mvn -pl user-service -am clean package
mvn -pl parcel-service -am clean package
```

Detalii arhitectura si matrice de independenta: [docs/architecture.md](D:\autopulse-both\docs\architecture.md)

## 8. Verificare config/port/build/database

- Config separat: prezent in fiecare serviciu
- Port separat: prezent in fiecare serviciu
- Build separat: prezent in fiecare serviciu
- Izolare date:
  - script SQL pentru DB-uri separate: `AutoPulse-backend/infra/postgres/sql/01-create-microservice-database.sql`
  - DB-uri dedicate: `user_db`, `parcel_db`, `fleet_db`, `routing_db`, `delivery_db`, `geography_db`, `notification_db`, `keycloak_db`

## 9. Rulare locala

### 9.1 Rulare backend complet (compose)

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

### 9.2 Rulare frontend

```bash
cd AutoPulse-frontend
npm install
npm run dev
```

- Frontend: `http://localhost:5173`
- Gateway: `http://localhost:8080`

### 9.3 Rulare individuala serviciu (exemplu)

```bash
cd AutoPulse-backend
mvn -pl parcel-service -am spring-boot:run
```

## 10. Trade-offs (rezumat)

Avantaje:

- deploy independent per bounded context
- scalare independenta
- fault isolation mai buna

Costuri:

- complexitate operationala mai mare
- consistenta distribuita mai dificila
- mai multe variabile de configurare

Detaliere: [docs/trade-offs.md](D:\autopulse-both\docs\trade-offs.md)

## 11. Schimbari minime aplicate in repository

Pentru claritate academica si rulare locala:

- actualizat `AutoPulse-backend/.env.example` pentru consistenta cu `docker-compose.yml`
- adaugat provisioning `keycloak_db` si `keycloak_user` in scriptul Postgres
- rescris documentatia in format evaluabil pentru cerinta de laborator

## 12. Checklist de conformitate cu enuntul

- Monolit explicat: DA
- Cel putin 2 bounded contexts separate: DA (`user-service`, `parcel-service`)
- Servicii independente deployable: DA
- Strategie de decompozitie documentata: DA
- Trade-offs documentate: DA
