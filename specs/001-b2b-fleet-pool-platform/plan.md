# Implementation Plan: B2B Rent-A-Car Filo Yönetimi ve Ortak Araç Havuzu Platformu

**Branch**: `001-b2b-fleet-pool-platform` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-b2b-fleet-pool-platform/spec.md`

## Summary

B2B rent-a-car platformu; dükkan sahiplerinin (Agency Owner) e-posta aktivasyonlu hesap kaydı, çoklu
şube yönetimi, araç filo envanteri ve sigorta/muayene operasyonel takibi ile birlikte, araçların
diğer dükkanların görebileceği ortak bir havuza (B2B Pool Marketplace) açılmasını sağlar.

Teknik yaklaşım: Gradle multi-project monorepo içinde 4 bağımsız Spring Boot 3.x mikroservis
(identity-agency, fleet, pool, notification) + Spring Cloud Gateway API Gateway. React + TypeScript +
Tailwind CSS SPA frontend. Servisler arası asenkron iletişim Apache Kafka, her servis bağımsız
PostgreSQL veritabanı (Database-per-service), şema versiyonlaması Flyway, stateless JWT kimlik
doğrulama Gateway'de yapılıp downstream'e header propagation ile aktarılır.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.x (Backend) | TypeScript 5.x, React 18+ (Frontend)

**Primary Dependencies**:
- Backend: Spring Boot 3.x, Spring Cloud Gateway, Spring Security (JWT), Spring Kafka, Spring Data JPA (Hibernate), Flyway, Micrometer Tracing (OpenTelemetry), Spring Boot Starter Mail
- Frontend: Vite, React 18+, TypeScript, Tailwind CSS, React Router v6, TanStack Query v5, Axios
- Infrastructure: PostgreSQL 16, Apache Kafka (KRaft), MailHog/Mailpit

**Storage**: PostgreSQL 16 — database-per-service: `identity_db`, `fleet_db`, `pool_db`

**Testing**: JUnit 5, AssertJ, Mockito, Testcontainers (PostgreSQL & Kafka), Vitest + React Testing Library (frontend)

**Target Platform**: Linux/Docker containers (development via Docker Compose on developer machines)

**Project Type**: Microservices web platform (multi-service backend + SPA frontend)

**Performance Goals**: Havuz durum yansıması ≤5s, filtreleme sonuçları <2s (100+ araç), aktivasyon e-postası ≤60s

**Constraints**: Stateless JWT (no server-side sessions), RFC 7807 error format, Flyway-only schema changes, idempotent Kafka consumers with DLT+retry

**Scale/Scope**: Başlangıç ölçeği: yüzlerce dükkan sahibi, binlerce araç. 4 mikroservis + 1 gateway + 1 SPA

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principle | Compliance | Status |
|---|-----------|------------|--------|
| I | Bağımsız Dağıtım ve SPA Mimarisi | 4 bağımsız Spring Boot servisi + Spring Cloud Gateway. React + Tailwind SPA. Servisler arası derleme bağımlılığı yalnızca ortak `common-dto` ve `common-events` kütüphanelerine (shared libraries, servis değil). | ✅ PASS |
| II | Veri İzolasyonu ve Şema Yönetimi | `identity_db`, `fleet_db`, `pool_db` — her servis bağımsız veritabanı. Paylaşılan tablo yok. Servisler arası veri akışı Kafka event ve HTTP API ile. Tüm şemalar Flyway ile versiyonlanır. | ✅ PASS |
| III | Stateless Güvenlik ve Kimlik Doğrulama | JWT üretimi `identity-agency-service`'de; doğrulama `api-gateway`'de. Gateway doğrulanmış context'i `X-User-Id`, `X-Agency-Ids` header'ları ile downstream'e aktarır. Oturum durumu tutulmaz. | ✅ PASS |
| IV | Asenkron İletişim ve Mesaj Dayanıklılığı | 4 Kafka topic domain standardında adlandırılır (`identity.user.registered`, `identity.agency.upserted`, `fleet.vehicle.status-changed`, `fleet.vehicle.compliance-alert`). Tüm consumer'lar idempotent, DLT + retry konfigüre edilir. | ✅ PASS |
| V | Dağıtık İzlenebilirlik | Micrometer Tracing / OpenTelemetry ile tüm HTTP ve Kafka trafiğinde `traceId`/`spanId` taşınır. Structured JSON loglama (Logback JSON encoder). | ✅ PASS |
| VI | Standart Hata Yönetimi | `common-dto` modülünde RFC 7807 `ProblemDetail` base class; tüm servisler `@ControllerAdvice` ile `application/problem+json` döner. `invalidParams` validasyon detayları dahil. | ✅ PASS |
| VII | Gerçekçi Entegrasyon Testleri | Testcontainers ile PostgreSQL ve Kafka tabanlı entegrasyon testleri. H2 veya embedded broker kullanılmaz. | ✅ PASS |

**Gate Result**: ✅ ALL PASS — Phase 0'a geçilebilir.

## Project Structure

### Documentation (this feature)

```text
specs/001-b2b-fleet-pool-platform/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── identity-agency-api.md
│   ├── fleet-api.md
│   ├── pool-api.md
│   └── kafka-events.md
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

```text
backend/
├── build.gradle                      # Root Gradle build (multi-project)
├── settings.gradle                   # Subproject includes
├── gradle.properties                 # Shared versions & config
├── common/
│   ├── common-dto/
│   │   └── src/main/java/.../dto/    # ProblemDetail, PageResponse, ApiEnvelope
│   └── common-events/
│       └── src/main/java/.../event/  # EventEnvelope<T>, domain event records
├── services/
│   ├── api-gateway/
│   │   ├── build.gradle
│   │   └── src/main/
│   │       ├── java/.../gateway/     # JwtAuthFilter, RouteConfig
│   │       └── resources/
│   │           └── application.yml
│   ├── identity-agency-service/
│   │   ├── build.gradle
│   │   └── src/
│   │       ├── main/java/.../identity/
│   │       │   ├── controller/       # AuthController, AgencyController
│   │       │   ├── service/          # AuthService, AgencyService, JwtService
│   │       │   ├── model/            # User, Agency, ActivationToken entities
│   │       │   ├── repository/       # JPA repositories
│   │       │   ├── event/            # Kafka producers
│   │       │   └── config/           # Security, Kafka, Flyway config
│   │       ├── main/resources/
│   │       │   ├── application.yml
│   │       │   └── db/migration/     # Flyway: V1__create_users.sql, ...
│   │       └── test/java/            # Unit + Testcontainers integration
│   ├── fleet-service/
│   │   ├── build.gradle
│   │   └── src/
│   │       ├── main/java/.../fleet/
│   │       │   ├── controller/       # VehicleController
│   │       │   ├── service/          # VehicleService, ComplianceService
│   │       │   ├── model/            # Vehicle, VehicleInspection entities
│   │       │   ├── repository/
│   │       │   ├── event/            # Kafka producers
│   │       │   └── config/
│   │       ├── main/resources/
│   │       │   ├── application.yml
│   │       │   └── db/migration/     # Flyway: V1__create_vehicles.sql, ...
│   │       └── test/java/
│   ├── pool-service/
│   │   ├── build.gradle
│   │   └── src/
│   │       ├── main/java/.../pool/
│   │       │   ├── controller/       # PoolController (read-only queries)
│   │       │   ├── service/          # PoolListingService
│   │       │   ├── model/            # PoolListing entity (denormalized)
│   │       │   ├── repository/
│   │       │   ├── consumer/         # Kafka consumers (idempotent)
│   │       │   └── config/
│   │       ├── main/resources/
│   │       │   ├── application.yml
│   │       │   └── db/migration/     # Flyway: V1__create_agency_projections.sql, V2__create_pool_listings.sql
│   │       └── test/java/
│   └── notification-service/
│       ├── build.gradle
│       └── src/
│           ├── main/java/.../notification/
│           │   ├── service/          # EmailService (activation, compliance alerts)
│           │   ├── consumer/         # Kafka consumers
│           │   ├── template/         # Email templates (Thymeleaf)
│           │   └── config/
│           ├── main/resources/
│           │   └── application.yml
│           └── test/java/

frontend/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.js
├── postcss.config.js
├── index.html
├── src/
│   ├── main.tsx                      # App entry
│   ├── App.tsx                       # Router setup
│   ├── api/                          # Axios instance, interceptors
│   │   └── client.ts
│   ├── auth/                         # Login, Register, Activate pages
│   │   ├── pages/
│   │   ├── hooks/
│   │   └── services/
│   ├── agency/                       # Agency CRUD, AgencySwitcher
│   │   ├── pages/
│   │   ├── components/
│   │   ├── hooks/
│   │   └── services/
│   ├── fleet/                        # Vehicle CRUD, status toggle, compliance badges
│   │   ├── pages/
│   │   ├── components/
│   │   ├── hooks/
│   │   └── services/
│   ├── pool/                         # Pool marketplace, filters, contact card modal
│   │   ├── pages/
│   │   ├── components/
│   │   ├── hooks/
│   │   └── services/
│   ├── shared/                       # Layout, Navbar, AgencySwitcher, ProtectedRoute
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── context/                  # AuthContext, AgencyContext
│   │   └── types/
│   └── lib/                          # Utilities, constants, error handling
└── tests/

docker/
├── docker-compose.yml                # PostgreSQL (x3), Kafka (KRaft), MailHog
└── .env.example                      # Environment variable template
```

**Structure Decision**: Monorepo with Gradle multi-project backend (5 subprojects: gateway + 4
domain services + 2 common libraries) and Vite React SPA frontend. Docker Compose for local
infrastructure. Each service has isolated `db/migration/` directory for Flyway, isolated
`test/` for Testcontainers integration tests. Frontend organized by domain feature modules
(auth, agency, fleet, pool) following feature-based architecture.

## Complexity Tracking

> No Constitution Check violations detected. No complexity justification required.
