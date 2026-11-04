# Tasks: B2B Rent-A-Car Filo Yönetimi ve Ortak Araç Havuzu Platformu

**Input**: Design documents from `/specs/001-b2b-fleet-pool-platform/`
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/)
**Organization**: Tasks are grouped by user story (US1–US5) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`, `[US4]`, `[US5]`)
- Setup, Foundational, and Polish phases have NO story label

---

## Phase 1: Setup (Shared Infrastructure & Monorepo Initialization)

**Purpose**: Project initialization, monorepo structure, and container orchestration

- [X] T001 Initialize root repository configuration and monorepo structure with `.gitignore` and `docker/docker-compose.yml` (PostgreSQL container with `identity_db`, `fleet_db`, `pool_db`, Apache Kafka KRaft on port 9092, MailHog on ports 1025/8025)
- [X] T002 [P] Initialize Gradle multi-project backend structure in `backend/settings.gradle`, `backend/build.gradle`, and `backend/gradle.properties` (Java 21, Spring Boot 3.3+, Spring Cloud 2023.x dependency management)
- [X] T003 [P] Initialize Vite React TypeScript frontend with Tailwind CSS in `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tailwind.config.js`, `frontend/postcss.config.js`, and `frontend/tsconfig.json`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core cross-cutting libraries, error models, event envelopes, gateway routing, and frontend client setup

⚠️ **CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 [P] Create `common-dto` module in `backend/common/common-dto/build.gradle` and implement RFC 7807 error models (`ProblemDetail` base with `type`, `title`, `status`, `detail`, `instance`, `invalidParams`), pagination (`PageResponse<T>`), and standard API envelopes in `backend/common/common-dto/src/main/java/com/rentacar/common/dto/`
- [X] T005 [P] Create `common-events` module in `backend/common/common-events/build.gradle` and implement generic `EventEnvelope<T>` record with fields `UUID eventId`, `String eventType`, `Instant occurredOn`, `UUID correlationId`, and `T payload` in `backend/common/common-events/src/main/java/com/rentacar/common/events/EventEnvelope.java`
- [X] T006 [P] Configure structured JSON logging with `logstash-logback-encoder` and Micrometer Tracing (W3C `traceparent` propagation) across backend services via shared configuration in `backend/common/common-dto/src/main/resources/logback-spring.xml`
- [X] T007 Configure Spring Cloud Gateway in `backend/services/api-gateway/build.gradle` and `backend/services/api-gateway/src/main/resources/application.yml` with routes to identity service (8081), fleet service (8082), and pool service (8083)
- [X] T008 Implement `JwtAuthFilter` in `backend/services/api-gateway/src/main/java/com/rentacar/gateway/filter/JwtAuthFilter.java` to validate `Authorization: Bearer <jwt>`, extract `userId`, `email`, and `agencyIds` claims, and propagate `X-User-Id` and `X-Agency-Ids` headers to downstream services
- [X] T009 [P] Setup frontend HTTP client and interceptors in `frontend/src/api/client.ts` with Axios for JWT token attachment (`Authorization: Bearer <token>`) and RFC 7807 Problem Details error parsing
- [X] T010 [P] Setup frontend base routing and application shell in `frontend/src/App.tsx`, `frontend/src/shared/components/Layout.tsx`, and `frontend/src/shared/components/Navbar.tsx`

**Checkpoint**: Shared modules, API Gateway, Docker infrastructure, and frontend baseline ready. User story implementation can begin.

---

## Phase 3: User Story 1 - Hesap Kaydı ve E-posta Aktivasyonu (Priority: P1) 🎯 MVP

**Goal**: Rent a car dükkan sahipleri hesap kaydı oluşturur, aktivasyon e-postasındaki bağlantıyla hesabını aktif eder ve sisteme giriş yaparak JWT token alır. Hesap aktif olmadan sisteme giriş yapılamaz.

**Independent Test**: Register formu doldurulur -> `identity_db.users` tablosuna `PENDING` statüsüyle kaydedilir -> Kafka'ya `identity.user.registered` event'i fırlatılır -> MailHog'a aktivasyon e-postası düşer -> `/activate?token={uuid}` çağrılır -> kullanıcı `ACTIVE` statüsüne geçer -> Login yapılarak geçerli JWT token alınır.

### Database & Models for User Story 1
- [X] T011 [P] [US1] Create Flyway migrations `V1__create_users_table.sql` and `V2__create_activation_tokens_table.sql` with verbatim constraints (`users`: `id UUID PK`, `first_name VARCHAR(100) NOT NULL`, `last_name VARCHAR(100) NOT NULL`, `email VARCHAR(255) NOT NULL UNIQUE`, `password_hash VARCHAR(255) NOT NULL`, `status VARCHAR(20) NOT NULL default 'PENDING'`, `created_at TIMESTAMPTZ NOT NULL`; `activation_tokens`: `id UUID PK`, `user_id UUID NOT NULL FK`, `token VARCHAR(255) NOT NULL UNIQUE`, `expires_at TIMESTAMPTZ NOT NULL`, `used BOOLEAN NOT NULL default false`) in `backend/services/identity-agency-service/src/main/resources/db/migration/`
- [X] T012 [P] [US1] Create `User` and `ActivationToken` JPA entities with validation constraints verbatim in `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/model/User.java` and `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/model/ActivationToken.java`
- [X] T013 [P] [US1] Implement `UserRepository` and `ActivationTokenRepository` in `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/repository/UserRepository.java` and `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/repository/ActivationTokenRepository.java`
- [X] T014 [P] [US1] Implement `UserRegisteredPayload` record in `backend/common/common-events/src/main/java/com/rentacar/common/events/identity/UserRegisteredPayload.java` with fields `UUID userId`, `String email`, `String firstName`, `String lastName`, `String activationToken`

### Backend Services & Endpoints for User Story 1
- [X] T015 [US1] Implement `JwtService` and `AuthService` in `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/service/JwtService.java` and `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/service/AuthService.java` (BCrypt password hash, 24-hour activation token generation, account activation to ACTIVE, login validation enforcing ACTIVE status and generating JWT with `sub`, `email`, `agencyIds` claims, and token refresh)
- [X] T016 [US1] Implement Kafka producer for `identity.user.registered` topic in `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/event/IdentityEventProducer.java`
- [X] T017 [US1] Implement `AuthController` in `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/controller/AuthController.java` exposing `POST /api/auth/register`, `POST /api/auth/activate`, `POST /api/auth/resend-activation`, `POST /api/auth/login`, and `POST /api/auth/refresh`
- [X] T018 [US1] Configure `notification-service` with Spring Mail (SMTP localhost:1025) and Kafka consumer for `identity.user.registered` with idempotency dedup table, 3 retries (1s, 2s, 4s exponential backoff), and `identity.user.registered.DLT` in `backend/services/notification-service/src/main/java/com/rentacar/notification/`
- [X] T019 [US1] Implement activation email template and sender in `backend/services/notification-service/src/main/java/com/rentacar/notification/service/EmailService.java` sending link `{FRONTEND_URL}/activate?token={uuid}` via MailHog

### Integration Tests for User Story 1
- [X] T020 [P] [US1] Write Testcontainers integration test with real PostgreSQL and Kafka containers for register, event publish, token activation, and login flows in `backend/services/identity-agency-service/src/test/java/com/rentacar/identity/AuthIntegrationTest.java`

### Frontend Implementation for User Story 1
- [X] T021 [P] [US1] Implement `AuthContext` and authentication service in `frontend/src/auth/context/AuthContext.tsx` and `frontend/src/auth/services/authApi.ts` for managing user state, tokens, and login/register calls
- [X] T022 [P] [US1] Implement Register, Login, and Activation pages in `frontend/src/auth/pages/RegisterPage.tsx`, `frontend/src/auth/pages/LoginPage.tsx`, and `frontend/src/auth/pages/ActivatePage.tsx`
- [X] T023 [US1] Implement `ProtectedRoute` guard in `frontend/src/shared/components/ProtectedRoute.tsx` redirecting unauthenticated users to `/login`

**Checkpoint**: User Story 1 is complete and independently testable as the platform's Minimum Viable Product (MVP).

---

## Phase 4: User Story 2 - Çoklu Dükkan Yönetimi (Priority: P1)

**Goal**: Dükkan sahipleri tek bir ana hesap üzerinden birden fazla şube/acente ekler, günceller, dükkanlar arasında arayüzde geçiş yapar. Dükkan oluşturulduğunda veya güncellendiğinde `identity.agency.upserted` Kafka olayı üretilir ve yeni dükkan ID'sini içeren güncel JWT token kullanıcıya döner.

**Independent Test**: Giriş yapmış kullanıcı `POST /api/agencies` ile dükkan ekler -> Yanıtta yeni dükkan nesnesi ve güncellenmiş `refreshedToken` döner -> Frontend auth context güncellenir (`X-Agency-Ids` yeni dükkanı içerir) -> Dükkan güncellendiğinde Kafka'da `identity.agency.upserted` event'i yayınlanır -> Frontend'te Agency Switcher ile dükkanlar arasında geçiş yapılır.

### Database & Models for User Story 2
- [X] T024 [P] [US2] Create Flyway migration `V3__create_agencies_table.sql` with verbatim constraints (`id UUID PK`, `owner_id UUID NOT NULL FK users(id) ON DELETE CASCADE`, `name VARCHAR(200) NOT NULL`, `city VARCHAR(100) NOT NULL`, `district VARCHAR(100) NULL`, `phone VARCHAR(20) NOT NULL`, `contact_person VARCHAR(200) NOT NULL`, `address TEXT NOT NULL`, `created_at TIMESTAMPTZ NOT NULL`) in `backend/services/identity-agency-service/src/main/resources/db/migration/`
- [X] T025 [P] [US2] Create `Agency` JPA entity with validation constraints verbatim in `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/model/Agency.java`
- [X] T026 [P] [US2] Implement `AgencyRepository` in `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/repository/AgencyRepository.java`
- [X] T027 [P] [US2] Implement `AgencyUpsertedPayload` record in `backend/common/common-events/src/main/java/com/rentacar/common/events/identity/AgencyUpsertedPayload.java` with fields `UUID agencyId`, `String name`, `String city`, `String district`, `String phone`, `String contactPerson`, `String address`

### Backend Services & Endpoints for User Story 2
- [X] T028 [US2] Implement `AgencyService` in `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/service/AgencyService.java` with ownership check (`owner_id = currentUserId`), CRUD operations, Kafka event publishing on creation and update to `identity.agency.upserted`, token regeneration logic (`JwtService.generateToken` with updated `agencyIds`), and agency deletion guard (verifying agency has no active vehicles or returning 409 Conflict)
- [X] T029 [US2] Implement `AgencyController` in `backend/services/identity-agency-service/src/main/java/com/rentacar/identity/controller/AgencyController.java` with endpoints `POST /api/agencies` (returning agency + `refreshedToken`), `GET /api/agencies`, `GET /api/agencies/{agencyId}`, `PUT /api/agencies/{agencyId}`, and `DELETE /api/agencies/{agencyId}` (returning `refreshedToken`)

### Integration Tests for User Story 2
- [X] T030 [P] [US2] Write Testcontainers integration test with real PostgreSQL and Kafka containers for agency CRUD, ownership enforcement, token refresh verification, and Kafka upsert event emission in `backend/services/identity-agency-service/src/test/java/com/rentacar/identity/AgencyIntegrationTest.java`

### Frontend Implementation for User Story 2
- [X] T031 [P] [US2] Implement `AgencyContext` and agency API service in `frontend/src/agency/context/AgencyContext.tsx` and `frontend/src/agency/services/agencyApi.ts` managing active selected agency, agency list, and integrating with `AuthContext` to immediately store `refreshedToken` upon agency creation/deletion so subsequent fleet requests include the new `agencyId` in `X-Agency-Ids` without 403 Forbidden
- [X] T032 [P] [US2] Implement `AgencySwitcher` dropdown in navbar and agency management views in `frontend/src/agency/components/AgencySwitcher.tsx` and `frontend/src/agency/pages/AgencyListPage.tsx`

**Checkpoint**: User Stories 1 and 2 work together. Users can register, activate, log in, create and switch agencies.

---

## Phase 5: User Story 3 - Filo Araç Yönetimi ve Operasyonel Takip (Priority: P2)

**Goal**: Dükkan sahipleri seçili dükkanlarına araç ekler, düzenler, siler. Sigorta ve muayene bitiş tarihlerini kaydeder; yaklaşan (≤30 gün) ve geçen tarihler için arayüzde renkli rozetler ve e-posta uyarıları alır.

**Independent Test**: Seçili dükkana araç eklenir -> `fleet_db.vehicles` ve `fleet_db.vehicle_inspections` tablolarına kaydedilir -> Plaka çakışması reddedilir -> Tarihi 15 gün kalan araç sarı `EXPIRING SOON`, geçmiş araç kırmızı `EXPIRED` badge alır -> Scheduled compliance job çalıştırıldığında `fleet.vehicle.compliance-alert` event'i yayınlanır -> MailHog'a uyarı e-postası düşer.

### Database & Models for User Story 3
- [X] T033 [P] [US3] Create Flyway migrations `V1__create_vehicles_table.sql` and `V2__create_vehicle_inspections_table.sql` with verbatim constraints (`vehicles`: `id UUID PK`, `agency_id UUID NOT NULL`, `plate VARCHAR(15) NOT NULL`, `brand VARCHAR(50) NOT NULL`, `model VARCHAR(50) NOT NULL`, `model_year INTEGER NOT NULL`, `fuel_type VARCHAR(20) NOT NULL enum ['GASOLINE', 'DIESEL', 'ELECTRIC', 'HYBRID']`, `transmission VARCHAR(20) NOT NULL enum ['MANUAL', 'AUTOMATIC']`, `daily_pool_price DECIMAL(10,2) NOT NULL > 0`, `status VARCHAR(20) NOT NULL default 'AVAILABLE'`, `UNIQUE(agency_id, plate)`; `vehicle_inspections`: `id UUID PK`, `vehicle_id UUID NOT NULL UNIQUE FK`, `traffic_insurance_expiry DATE NULL`, `casco_expiry DATE NULL`, `inspection_expiry DATE NULL`) in `backend/services/fleet-service/src/main/resources/db/migration/`
- [X] T034 [P] [US3] Create `Vehicle` and `VehicleInspection` JPA entities with validation constraints verbatim in `backend/services/fleet-service/src/main/java/com/rentacar/fleet/model/Vehicle.java` and `backend/services/fleet-service/src/main/java/com/rentacar/fleet/model/VehicleInspection.java`
- [X] T035 [P] [US3] Implement `VehicleRepository` and `VehicleInspectionRepository` in `backend/services/fleet-service/src/main/java/com/rentacar/fleet/repository/VehicleRepository.java` and `backend/services/fleet-service/src/main/java/com/rentacar/fleet/repository/VehicleInspectionRepository.java`
- [X] T036 [P] [US3] Implement `VehicleComplianceAlertPayload` record in `backend/common/common-events/src/main/java/com/rentacar/common/events/fleet/VehicleComplianceAlertPayload.java`

### Backend Services & Endpoints for User Story 3
- [X] T037 [US3] Implement `VehicleService` in `backend/services/fleet-service/src/main/java/com/rentacar/fleet/service/VehicleService.java` with ownership check (`agencyId ∈ X-Agency-Ids`), unique plate check within agency, and CRUD with inspection date tracking
- [X] T038 [US3] Implement `VehicleComplianceScheduler` in `backend/services/fleet-service/src/main/java/com/rentacar/fleet/scheduler/VehicleComplianceScheduler.java` with daily cron checking expiry dates ≤30 days or expired, publishing events to `fleet.vehicle.compliance-alert`
- [X] T039 [US3] Implement Kafka consumer in `notification-service` for `fleet.vehicle.compliance-alert` with retry and `fleet.vehicle.compliance-alert.DLT` in `backend/services/notification-service/src/main/java/com/rentacar/notification/consumer/ComplianceAlertConsumer.java`
- [X] T040 [US3] Implement `VehicleController` in `backend/services/fleet-service/src/main/java/com/rentacar/fleet/controller/VehicleController.java` with endpoints `POST /api/vehicles`, `GET /api/vehicles`, `GET /api/vehicles/{vehicleId}`, `PUT /api/vehicles/{vehicleId}`, and `DELETE /api/vehicles/{vehicleId}`

### Integration Tests for User Story 3
- [X] T041 [P] [US3] Write Testcontainers integration test with PostgreSQL and Kafka containers for vehicle CRUD, plate uniqueness per agency, and compliance cron job in `backend/services/fleet-service/src/test/java/com/rentacar/fleet/VehicleIntegrationTest.java`

### Frontend Implementation for User Story 3
- [X] T042 [P] [US3] Implement fleet API service and TanStack Query hooks in `frontend/src/fleet/services/fleetApi.ts` and `frontend/src/fleet/hooks/useVehicles.ts`
- [X] T043 [P] [US3] Implement fleet list view, vehicle create/edit modal with inspection date pickers, and compliance badges (EXPIRED, EXPIRING_SOON) in `frontend/src/fleet/pages/FleetListPage.tsx`, `frontend/src/fleet/components/VehicleModal.tsx`, and `frontend/src/fleet/components/ComplianceBadge.tsx`

**Checkpoint**: User Stories 1, 2, and 3 are functional. Fleets can be managed with inspection tracking.

---

## Phase 6: User Story 4 - Araç Durum Yönetimi (Priority: P2)

**Goal**: Dükkan sahipleri araç durumunu tek tıkla `Uygun` (AVAILABLE), `Dolu` (RENTED) veya `Havuzda` (IN_POOL) olarak değiştirir. Durum `IN_POOL` olduğunda Kafka üzerinden pool-service'e bildirilir ve ortak havuza yansıtılır; durum değiştiğinde havuzdan kaldırılır.

**Independent Test**: Araç durumu `PATCH /api/vehicles/{id}/status` ile `IN_POOL` yapılır -> Kafka'da `fleet.vehicle.status-changed` event'i yayınlanır -> `pool_db.pool_listings` tablosuna araç eklenir -> Durum `AVAILABLE` yapıldığında `pool_db.pool_listings` tablosundan silinir.

### Event Records & Fleet Backend for User Story 4
- [X] T044 [P] [US4] Implement `VehicleStatusChangedPayload` record in `backend/common/common-events/src/main/java/com/rentacar/common/events/fleet/VehicleStatusChangedPayload.java` with vehicle fields (`UUID vehicleId`, `UUID agencyId`, `String plate`, `String brand`, `String model`, `int modelYear`, `String fuelType`, `String transmission`, `BigDecimal dailyPoolPrice`, `String newStatus`, `String previousStatus`) keeping fleet-service decoupled from agency contact details
- [X] T045 [US4] Implement vehicle status update logic and Kafka event emission in `backend/services/fleet-service/src/main/java/com/rentacar/fleet/service/VehicleService.java`
- [X] T046 [US4] Implement status update endpoint `PATCH /api/vehicles/{vehicleId}/status` in `backend/services/fleet-service/src/main/java/com/rentacar/fleet/controller/VehicleController.java`

### Pool Service Database & Consumer for User Story 4
- [X] T047 [P] [US4] Create Flyway migrations `V1__create_agency_projections_table.sql`, `V2__create_pool_listings_table.sql`, and `V3__create_processed_events_table.sql` with verbatim constraints (`agency_projections`: `agency_id UUID PK`, `name VARCHAR(200) NOT NULL`, `city VARCHAR(100) NOT NULL`, `district VARCHAR(100) NULL`, `phone VARCHAR(20) NOT NULL`, `contact_person VARCHAR(200) NOT NULL`, `address TEXT NOT NULL`, `updated_at TIMESTAMPTZ NOT NULL`; `pool_listings`: `id UUID PK`, `vehicle_id UUID NOT NULL UNIQUE`, `agency_id UUID NOT NULL`, `agency_name VARCHAR(200) NOT NULL`, `agency_city VARCHAR(100) NOT NULL`, `agency_phone VARCHAR(20) NOT NULL`, `contact_person VARCHAR(200) NOT NULL`, `agency_address TEXT NOT NULL`, `plate VARCHAR(15) NOT NULL`, `brand VARCHAR(50) NOT NULL`, `model VARCHAR(50) NOT NULL`, `model_year INTEGER NOT NULL`, `fuel_type VARCHAR(20) NOT NULL`, `transmission VARCHAR(20) NOT NULL`, `daily_pool_price DECIMAL(10,2) NOT NULL`, `listed_at TIMESTAMPTZ NOT NULL`; `processed_events`: `event_id UUID PK`, `topic VARCHAR(100) NOT NULL`, `processed_at TIMESTAMPTZ NOT NULL`) in `backend/services/pool-service/src/main/resources/db/migration/`
- [X] T048 [P] [US4] Create `AgencyProjection`, `PoolListing`, and `ProcessedEvent` JPA entities with validation constraints verbatim in `backend/services/pool-service/src/main/java/com/rentacar/pool/model/AgencyProjection.java`, `backend/services/pool-service/src/main/java/com/rentacar/pool/model/PoolListing.java`, and `backend/services/pool-service/src/main/java/com/rentacar/pool/model/ProcessedEvent.java`
- [X] T049 [P] [US4] Implement `AgencyProjectionRepository`, `PoolListingRepository`, and `ProcessedEventRepository` in `backend/services/pool-service/src/main/java/com/rentacar/pool/repository/AgencyProjectionRepository.java`, `backend/services/pool-service/src/main/java/com/rentacar/pool/repository/PoolListingRepository.java`, and `backend/services/pool-service/src/main/java/com/rentacar/pool/repository/ProcessedEventRepository.java`
- [X] T050 [US4] Implement idempotent Kafka consumer for `fleet.vehicle.status-changed` in `backend/services/pool-service/src/main/java/com/rentacar/pool/consumer/VehicleStatusConsumer.java` (dedup via `processed_events`, on `IN_POOL` queries local `AgencyProjection` by `agencyId` to assemble and upsert `PoolListing`, on `AVAILABLE`/`RENTED`/`DELETED` deletes from `pool_listings`, with 3 retries and `fleet.vehicle.status-changed.DLT`)

### Integration Tests & Frontend for User Story 4
- [X] T051 [P] [US4] Write Testcontainers integration test for vehicle status transition, Kafka event publishing, and pool listing upsert/delete in `backend/services/fleet-service/src/test/java/com/rentacar/fleet/VehicleStatusIntegrationTest.java`
- [X] T052 [P] [US4] Implement single-click status toggle component in `frontend/src/fleet/components/StatusSwitcher.tsx` with optimistic UI update and TanStack Query cache invalidation

**Checkpoint**: Status changes immediately reflect across services via Kafka events and denormalized pool listings.

---

## Phase 7: User Story 5 - Ortak Araç Havuzu Görüntüleme ve İletişim (Priority: P3)

**Goal**: Doğrulanmış dükkan sahipleri ortak araç havuzundaki araçları listeler, filtreler (şehir, marka, yakıt, vites, fiyat), kendi araçlarını hariç tutar ve ilgilendikleri aracın dükkan iletişim kartını görüntüler.

**Independent Test**: Dükkan A aracı havuza açar -> Dükkan B havuz ekranını açar -> Dükkan A'nın aracı listelenir (Dükkan B'nin kendi araçları filtrelenir) -> Şehir/marka/yakıt filtresi uygulanır -> Dükkan kartına tıklanarak telefon, sorumlu kişi ve adres görüntülenir.

### Backend Services & Endpoints for User Story 5
- [X] T053 [US5] Implement idempotent Kafka consumer for `identity.agency.upserted` in `backend/services/pool-service/src/main/java/com/rentacar/pool/consumer/AgencyUpsertedConsumer.java` upserting local `AgencyProjection` table and updating denormalized agency contact info across all pool listings matching `agency_id` (with 3 retries and `identity.agency.upserted.DLT`)
- [X] T054 [US5] Implement `PoolListingService` in `backend/services/pool-service/src/main/java/com/rentacar/pool/service/PoolListingService.java` with filtering by city, brand, fuelType, transmission, price range, dynamic filter facets (`GET /api/pool/filters`), and exclusion of user's own agencies (`agency_id NOT IN (:excludedAgencyIds)`)
- [X] T055 [US5] Implement `PoolController` in `backend/services/pool-service/src/main/java/com/rentacar/pool/controller/PoolController.java` with endpoints `GET /api/pool/listings`, `GET /api/pool/listings/{vehicleId}`, and `GET /api/pool/filters`

### Integration Tests for User Story 5
- [X] T056 [P] [US5] Write Testcontainers integration test with PostgreSQL and Kafka containers for pool queries, filtering, dynamic facets, and agency exclusion in `backend/services/pool-service/src/test/java/com/rentacar/pool/PoolIntegrationTest.java`

### Frontend Implementation for User Story 5
- [X] T057 [P] [US5] Implement pool marketplace API service and TanStack Query hooks in `frontend/src/pool/services/poolApi.ts` and `frontend/src/pool/hooks/usePoolListings.ts`
- [X] T058 [P] [US5] Implement pool marketplace page with search and filter sidebar in `frontend/src/pool/pages/PoolMarketplacePage.tsx` and `frontend/src/pool/components/PoolFilterSidebar.tsx`
- [X] T059 [P] [US5] Implement vehicle marketplace card and agency contact modal in `frontend/src/pool/components/PoolVehicleCard.tsx` and `frontend/src/pool/components/AgencyContactModal.tsx`

**Checkpoint**: All 5 user stories are complete. The entire B2B marketplace workflow operates end-to-end.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Global error handling validation, environment configuration, quickstart verification, and documentation

- [X] T060 [P] Implement global `@ControllerAdvice` RFC 7807 problem details exception handlers in `identity-agency-service`, `fleet-service`, and `pool-service` mapping domain exceptions (`EntityNotFoundException`, `DuplicateResourceException`, `AccessDeniedException`) to `application/problem+json` with `invalidParams`
- [X] T061 [P] Create Docker Compose environment template in `docker/.env.example` and healthcheck configs in `docker/docker-compose.yml`
- [X] T062 Validate end-to-end execution scenarios (Scenarios 1 through 6) from `specs/001-b2b-fleet-pool-platform/quickstart.md`
- [X] T063 Update project documentation and service setup guides in `README.md`

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1: Setup
    │
    ▼
Phase 2: Foundational (BLOCKING - Must finish before any story)
    │
    ├──────────────────────────┬──────────────────────────┐
    ▼                          ▼                          ▼
Phase 3: US1 (Auth & Reg)   Phase 4: US2 (Agencies)   Phase 5: US3 (Fleet)
[Priority: P1 - MVP]        [Priority: P1]            [Priority: P2]
    │                          │                          │
    └──────────────────────────┼──────────────────────────┘
                               │
                               ▼
                        Phase 6: US4 (Status & Kafka to Pool)
                        [Priority: P2]
                               │
                               ▼
                        Phase 7: US5 (Pool Marketplace)
                        [Priority: P3]
                               │
                               ▼
                        Phase 8: Polish & Verification
```

### User Story Dependencies

- **US1 (Auth & Activation - P1)**: Depends ONLY on Phase 2 (Foundational). Delivers working authentication and user activation.
- **US2 (Multi-Agency - P1)**: Depends on Phase 2. Uses user credentials from US1 for agency ownership.
- **US3 (Fleet Management - P2)**: Depends on Phase 2. Uses `agencyId` context established in US2.
- **US4 (Vehicle Status Management - P2)**: Depends on US3 (vehicles must exist) and Kafka infrastructure from Phase 2.
- **US5 (Pool Marketplace - P3)**: Depends on US4 (vehicles must be published to pool) and US2 (agency contact info replicated via `identity.agency.upserted` into `agency_projections`).

---

## Parallel Opportunities

### Parallel Opportunities within Phases

- **Phase 1 (Setup)**: T002 (Gradle) and T003 (Vite) can run concurrently after T001.
- **Phase 2 (Foundational)**: T004 (`common-dto`), T005 (`common-events`), T006 (Logging), T009 (Frontend client), and T010 (Frontend shell) can all execute in parallel.
- **Phase 3 (US1)**:
  - Database & Models: T011, T012, T013, T014 can be developed in parallel.
  - Test & Frontend: T020 (Testcontainers test), T021 (AuthContext), and T022 (Auth pages) can run in parallel with backend endpoints.
- **Phase 4 (US2)**:
  - T024, T025, T026, T027 can run in parallel.
  - T030 (Integration test), T031 (AgencyContext), and T032 (AgencySwitcher) can run in parallel.
- **Phase 5 (US3)**:
  - T033, T034, T035, T036 can run in parallel.
  - T041 (Integration test), T042 (Hooks), and T043 (Fleet UI) can run in parallel.
- **Phase 6 (US4)**:
  - T044, T047, T048, T049 can run in parallel.
  - T051 (Integration test) and T052 (StatusSwitcher UI) can run in parallel.
- **Phase 7 (US5)**:
  - T056 (Pool test), T057 (Pool hooks), T058 (Marketplace page), and T059 (Contact modal) can run in parallel.

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Complete Phase 1: Setup (T001–T003)
2. Complete Phase 2: Foundational (T004–T010)
3. Complete Phase 3: User Story 1 (T011–T023)
4. **STOP and VALIDATE**: Verify account registration, activation email via MailHog, account activation, and JWT login. This forms the independent MVP.

### Incremental Delivery Order
1. **Increment 1 (MVP)**: US1 (Auth & Activation) -> User can safely sign up and log in.
2. **Increment 2**: US2 (Multi-Agency) -> User can create and manage rental agencies/branches.
3. **Increment 3**: US3 (Fleet Management) -> User can add vehicles and track insurance/inspection dates.
4. **Increment 4**: US4 (Vehicle Status & Kafka) -> Vehicle status changes publish events to pool database.
5. **Increment 5**: US5 (Pool Marketplace) -> Cross-agency vehicle sharing and contact info browsing.
6. **Increment 6**: Polish & End-to-End Validation (Phase 8).

