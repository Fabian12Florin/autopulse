# AutoPulse Web Portal

Frontend portal for the AutoPulse operations platform, implemented with React + TypeScript + Vite.

## Scope implemented

- React + TypeScript + Vite application scaffold
- Authentication shell (login, logout, password reset)
- Role-aware UI behavior:
  - `ADMIN` can access all pages and admin console
  - `DISPATCHER` can access courier/route/parcel/fleet/geography operations
- Protected route flow
- Operations pages:
  - Dashboard
  - Users (admin)
  - Couriers
  - Parcels
  - Routes
  - Delivery Runs
  - Depots
  - Fleet
  - Geography
  - Admin Console
- Master-detail UX:
  - Select entity in list and inspect details panel
  - Manage status/availability/assignment from details actions
  - Cross-linked data: depot -> couriers/vehicles/parcels, route -> parcels, run -> route/courier/vehicle
- Backend integration and safe fallback strategy:
  - Uses live backend endpoints for auth and core operations entities
  - Syncs users, couriers, depots, fleet, parcels, and geography query data
  - Uses backend mutation endpoints where available (activate/deactivate, vehicle status/assignment, parcel lifecycle, run status)
  - Falls back to local in-memory data when backend services or endpoints are unavailable
- Very small CSS baseline (`src/styles/global.css`) for cards/tables/layout only
- Minimal `fetch` API client baseline with `VITE_API_BASE_URL`

## Prerequisites

- Node.js 20+
- npm 10+ (or equivalent package manager)

## Run locally

```bash
npm install
npm run dev
```

App starts on `http://localhost:5173`.

## Build

```bash
npm run build
npm run preview
```

## Environment variables

Create `.env` file:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

## Next integration steps

1. Add token refresh + retry-on-401 flow in `src/features/auth/AuthContext.tsx` / `src/api/httpClient.ts`.
2. Move backend DTO mapping from `OpsDataContext` into dedicated service modules per microservice.
3. Add create/edit forms for entities (users, couriers, depots, vehicles, parcels, geography).
4. Add tests for role-based route guarding and data-mapping adapters.
5. Add e2e smoke tests for every admin/dispatcher page flow.
