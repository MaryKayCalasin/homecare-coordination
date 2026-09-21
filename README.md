# Homecare Coordination

A home care coordination platform for Norwegian municipalities in real time scheduling, absence handling and visit redistribution for home care services. Built with Java 17 and Spring Boot 3

## Features

1. **Visit scheduling and coordination** — schedule, reschedule, assign, start, complete and cancel patient visits, with conflict detection so a nurse is never double-booked, checked alongside a travel-time heuristic so back-to-back visits at different addresses leave enough time to actually get there, and a qualification check so a nurse is only assigned a visit type (medication, wound care, personal care) their work-capacity flags cover.
2. **Nurse absence handling with automatic visit redistribution** — registering an absence immediately finds every affected visit and reassigns it to an available, qualified nurse in the same municipality, preferring one who has cared for that patient before (continuity of care) and otherwise the least-loaded; anything that can't be covered is left unassigned for a coordinator to resolve, and everyone is notified in real time.
3. **Patient observation journal** — medication administration, mood, vital signs and incident entries per patient, with an `urgent` flag that raises an immediate alert.
4. **Auto-generated shift handover reports** — a report summarizing completed/missed visits, medications given and unresolved urgent flags is generated automatically at every shift boundary (07:00 / 15:00 / 23:00 Europe/Oslo) and can also be triggered on demand.
5. **Real-time updates over WebSocket** — visit changes, urgent alerts, redistribution outcomes and new handover reports are pushed over STOMP/SockJS.
6. **Full GDPR audit trail** — every access to and change of patient data is recorded (who, what, when, from where) via an AOP aspect (`@Auditable`) plus explicit audit calls, independent of the triggering transaction.
7. **Kommune (municipality) tenant isolation** — every login account belongs to a municipality (`User.municipality`, nullable only for a platform-wide account), and `CurrentUser` enforces it on every patient/nurse/visit/absence/observation/handover-report read and write, regardless of role. A coordinator in one kommune cannot read or list another kommune's patients.
8. **Vedtak (statutory decision) foundation** — a `Vedtak` records the service type, granted weekly hours and validity window a kommune has formally decided a patient is entitled to (modeled after IPLOS service categories), and a visit can optionally link to the vedtak that justifies it. Tracking hours delivered against what was granted, and an actual IPLOS statistics export, are not implemented — this is the entity model and API shape, not the full entitlement system.

## Stack

Java 17 · Spring Boot 3.3 · Spring Data JPA · Flyway · Spring Security (JWT) · PostgreSQL · WebSocket (STOMP/SockJS) · Spring AOP · Docker Compose · Maven

## Database schema

Schema changes are managed by [Flyway](https://flywaydb.org) migrations under `src/main/resources/db/migration`, not by Hibernate. `spring.jpa.hibernate.ddl-auto` is set to `validate`: Hibernate checks the entity mappings match what Flyway created and fails fast at startup on a mismatch, rather than surfacing as a runtime error on first use.

To add a field to an entity, add a corresponding `V<n>__description.sql` migration in the same change. When adding a `NOT NULL` column to a table that may already have rows, include a `DEFAULT` in the same `ADD COLUMN` statement (e.g. `ADD COLUMN foo boolean NOT NULL DEFAULT false`) — PostgreSQL backfills existing rows from the default in that one statement. Adding a `NOT NULL` column with no default fails outright against a non-empty table.

`V1__baseline_schema.sql` is a snapshot of the schema as it existed when Flyway was introduced (originally created by Hibernate's `ddl-auto=update`). `spring.flyway.baseline-on-migrate` + `baseline-version: 1` let Flyway adopt an already-existing database transparently: on an existing database it treats V1 as already applied and only runs migrations after it; on a brand new database it runs V1 and everything after it, in order, ending at the same schema either way.

Tests run against H2 with `spring.flyway.enabled: false` (see `src/test/resources/application.yml`) and let Hibernate generate the schema directly from the entities via `ddl-auto: create-drop`, since the migrations are PostgreSQL-specific SQL.

## Project structure

```
src/main/java/no/kommune/homecare/
├── HomecareCoordinationApplication.java
├── common/            # BaseEntity, exceptions, global error handler
├── config/            # JPA auditing, dev data seeder
├── security/          # JWT auth, Spring Security config, login/register
├── audit/             # @Auditable annotation, aspect, AuditLog, GDPR audit API
├── user/              # Platform login accounts (ADMIN / COORDINATOR / NURSE)
├── patient/            # Patient records
├── nurse/              # Nurse records and qualifications
├── visit/              # Visit scheduling
├── absence/            # Nurse absences + automatic redistribution
├── observation/        # Patient observation journal
├── handover/            # Shift handover report generation
├── vedtak/              # Statutory decisions (entitlement foundation)
└── websocket/            # STOMP config + real-time event publisher
```

Each domain package follows the same shape: `Entity`, `Repository`, `Service`, `Controller`, and a `dto` sub-package for request/response records.

## Running with Docker Compose

```bash
docker compose up --build
```

This starts PostgreSQL and the app on `http://localhost:8080`, running in the `docker` profile against the containerized database. `APP_SEED_ENABLED=true` is set in `docker-compose.yml`, so on first startup (only if no users exist yet) it seeds the same starter dataset described below. Set that variable to `false` (or remove it) in `docker-compose.yml` once you point it at anything other than a throwaway local database.

## Running locally against your own PostgreSQL

```bash
export DB_HOST=localhost DB_PORT=5432 DB_NAME=homecare DB_USERNAME=homecare DB_PASSWORD=homecare_dev_password
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

The `dev` profile also enables `app.seed.enabled`, seeding a starter dataset on first run (only if no users exist yet):

| Username        | Password       | Role  |
|------------------|----------------|-------|
| `admin`          | `ChangeMe123!` | ADMIN |
| `kari.nordmann`  | `ChangeMe123!` | NURSE |

Seeding is controlled by the `app.seed.enabled` property (`APP_SEED_ENABLED` env var), not by profile name, so it can be turned on for any environment — it defaults to `false` everywhere except the `dev` profile and the Docker Compose file. Change these credentials before using anything beyond local development.

## Authentication

`POST /api/auth/login` with `{ "username": "...", "password": "..." }` returns a JWT. Send it as `Authorization: Bearer <token>` on subsequent requests. There is no public self-registration — `POST /api/auth/register` requires an authenticated ADMIN, since access to a municipality's patient data must be provisioned deliberately.

## WebSocket

Connect to `ws://localhost:8080/ws` (SockJS-compatible) and subscribe to:

- `/topic/visits` and `/topic/nurses/{nurseId}/visits` — visit lifecycle events
- `/topic/alerts/urgent` — urgent observation alerts
- `/topic/absences/redistribution` — outcome of automatic visit redistribution
- `/topic/handover-reports` — new shift handover reports

## GDPR audit trail

`GET /api/audit-logs` (ADMIN/COORDINATOR only) lists every recorded access and change, filterable by entity type, entity, or user. Entries are written in their own transaction (`REQUIRES_NEW`) so an audit record survives even if the triggering operation is rolled back.

## Kommune tenant isolation

Every login account (`User`) has a `municipality`. `no.kommune.homecare.security.CurrentUser` reads the authenticated principal straight from `SecurityContextHolder` and enforces that municipality on every patient, nurse, visit, absence, observation and handover-report read and write, regardless of role — a `municipality` left `null` on the account marks a platform-wide account (used for the initial bootstrap admin) that isn't scoped to any one kommune. List endpoints are filtered rather than rejected; single-resource endpoints (`GET /api/patients/{id}` and similar) return `403 Forbidden` for a resource outside the caller's municipality. `TenantIsolationTest` exercises this with a real `AuthenticatedUser` principal (not `@WithMockUser`'s generic one, which `CurrentUser` doesn't recognize) against a live `MockMvc` + H2 database.

## Vedtak (statutory decisions)

`POST /api/vedtak`, `GET /api/vedtak/{id}`, `GET /api/vedtak/by-patient/{patientId}`, `PATCH /api/vedtak/{id}/revoke` — records the administrative decision (`Vedtak`) that grants a patient a specific service under helse- og omsorgstjenesteloven: service type (modeled after IPLOS categories), granted hours per week, and a validity window. `PATCH /api/visits/{id}/link-vedtak/{vedtakId}` optionally links a visit to the vedtak that justifies it. This is the entity model and API shape, not a complete entitlement system — nothing here tracks hours actually delivered against what was granted, and there's no IPLOS statistics export.

## Tests

```bash
mvn test
```

31 tests: a Spring context load test (backed by an in-memory H2 database); focused unit tests for visit-conflict detection, qualification checks, and automatic visit-redistribution (including the "substitute found" and "no substitute available" cases, qualification filtering, and continuity-of-care preference); `TravelTimeTest` for the travel-feasibility heuristic; `TenantIsolationTest`, a `MockMvc` integration test proving cross-municipality access is actually blocked; and `OpenInViewRegressionTest`, pinning the `open-in-view: false` / `LazyInitializationException` fix so that regression can't come back silently.

## Continuous integration

`.github/workflows/ci.yml` runs on every push to `main` and every pull request: `mvn test` against a pinned JDK 17 (not whatever JDK a contributor has locally — see the Mockito/Byte Buddy/Lombok version notes in `pom.xml` if a very new local JDK breaks a build tool), plus `npm run lint` and `npm run build` for the frontend.

## Frontend

A React + Tailwind frontend lives under `frontend/`, in Norwegian, talking to this backend at `http://localhost:8080`:

```bash
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:5173` (already in `app.cors.allowed-origins` by default). Log in with one of the seeded accounts (see above). Features:

- **Dashbord** — today's visits (or any day, via the date navigator), with quick actions to start/complete/cancel a visit.
- **Kart** — patient locations for the day's visits on a Leaflet/OpenStreetMap map, colored by assigned nurse. Patient addresses are geocoded client-side via OpenStreetMap's Nominatim (rate-limited to ~1 req/sec, cached in `localStorage`); a first successful lookup for a patient is also `PATCH`ed back to `/api/patients/{id}/coordinates` so the backend's travel-time check has coordinates to work with, without the backend making any geocoding calls of its own.
- **Fravær** — register a nurse absence and watch the automatic visit redistribution outcome arrive live over WebSocket.
- Real-time updates throughout via STOMP/SockJS (`/topic/visits`, `/topic/absences/redistribution`, `/topic/alerts/urgent`, `/topic/handover-reports`).

This also adds `GET /api/visits?date=YYYY-MM-DD` (defaults to today, Europe/Oslo) to `VisitController`, exposing the existing `VisitService.findByRange` so the dashboard and map can list all of a day's visits — previously only per-patient/per-nurse listings existed.
