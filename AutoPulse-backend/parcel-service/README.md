# Parcel Service

Parcel Service manages parcel creation, AWB generation, parcel lifecycle transitions, payment collection requirements, routable parcel exposure, and delivery execution outcomes.

API title: **Parcel Service API**  
Version: **0.1**  
Base URL: `http://localhost:8083`

---

## Responsibility summary

This service is responsible for:

- creating parcels from external requests
- generating and storing AWB identifiers
- maintaining parcel lifecycle state
- storing receiver payment requirements
- exposing parcels for routing and operational search
- marking parcels as delivered, failed, rejected, returned, or waiting for pickup based on execution events

---

## Features

- Create parcel records
- Search parcels by operational criteria
- Retrieve parcel by internal ID or AWB
- Update parcel details
- Cancel parcels
- Track parcel lifecycle history
- Apply last-mile lifecycle transitions:
    - out for delivery
    - delivered
    - failed
    - rejected
    - returned
    - waiting for pickup

---

## API overview

### Main parcel endpoints

| Method | Endpoint                          | Description           |
|---|-----------------------------------|-----------------------|
| `POST` | `/api/parcels`                    | Create a parcel       |
| `PUT` | `/api/parcels/{parcelId}`         | Update parcel         |
| `GET` | `/api/parcels`                    | Search parcels        |
| `GET` | `/api/parcels/{parcelId}`         | Get parcel by ID      |
| `GET` | `/api/parcels/awb/{awb}`          | Get parcel by AWB     |
| `GET` | `/api/parcels/awb/{awb}/pin`      | Get parcel PIN by AWB |
| `GET` | `/api/parcels/{parcelId}/history` | Get parcel history    |

### Lifecycle endpoints

| Method | Endpoint | Description                |
|---|---|----------------------------|
| `POST` | `/api/parcels/{parcelId}/cancel` | Mark parcel cancelled      |
| `POST` | `/api/parcels/{parcelId}/out-for-delivery` | Mark parcel out for delivery |
| `POST` | `/api/parcels/{parcelId}/delivered` | Mark parcel delivered      |
| `POST` | `/api/parcels/{parcelId}/failed` | Mark parcel failed         |
| `POST` | `/api/parcels/{parcelId}/rejected` | Mark parcel rejected       |
| `POST` | `/api/parcels/{parcelId}/returned` | Mark parcel returned       |
| `POST` | `/api/parcels/{parcelId}/waiting-pickup` | Mark parcel waiting for pickup |