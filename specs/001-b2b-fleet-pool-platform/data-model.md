# Data Model: B2B Rent-A-Car Filo Yönetimi ve Ortak Araç Havuzu Platformu

**Phase**: 1 — Design & Contracts
**Date**: 2026-11-04
**Feature**: [spec.md](spec.md) | [plan.md](plan.md) | [research.md](research.md)

---

## Database Topology

```text
┌─────────────────────────┐   ┌─────────────────────────┐   ┌─────────────────────────┐
│      identity_db        │   │        fleet_db          │   │        pool_db          │
│  (identity-agency-svc)  │   │    (fleet-service)       │   │    (pool-service)       │
├─────────────────────────┤   ├─────────────────────────┤   ├─────────────────────────┤
│  users                  │   │  vehicles                │   │  agency_projections     │
│  activation_tokens      │   │  vehicle_inspections     │   │  pool_listings          │
│  agencies               │   │  processed_events        │   │  processed_events       │
└─────────────────────────┘   └─────────────────────────┘   └─────────────────────────┘
```

---

## Entity Definitions

### 1. User (identity_db.users)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK, default `gen_random_uuid()` | Benzersiz kullanıcı kimliği |
| `first_name` | `VARCHAR(100)` | NOT NULL | Kullanıcı adı |
| `last_name` | `VARCHAR(100)` | NOT NULL | Kullanıcı soyadı |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | E-posta adresi (login kimliği) |
| `password_hash` | `VARCHAR(255)` | NOT NULL | BCrypt ile hashlenmiş şifre |
| `status` | `VARCHAR(20)` | NOT NULL, default `'PENDING'` | Hesap durumu: `PENDING`, `ACTIVE` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | Kayıt tarihi |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | Son güncelleme tarihi |

**Indexes**: `UNIQUE(email)`

**Validation Rules**:
- `email`: Geçerli e-posta formatı, max 255 karakter
- `password`: Min 8 karakter (hash öncesi), en az 1 büyük harf, 1 küçük harf, 1 rakam
- `first_name`, `last_name`: Min 2 karakter, max 100 karakter
- `status`: Yalnızca `PENDING` veya `ACTIVE` değerlerini alabilir

**State Transitions**:
```text
[Kayıt] ──→ PENDING ──→ ACTIVE
                │            │
                │ (aktivasyon │ (hesap deaktivasyon
                │  bağlantısı │  ileride eklenebilir)
                │  doğrulanır)│
                ▼            ▼
           (tekrar aktivasyon
            e-postası talep edilebilir)
```

---

### 2. ActivationToken (identity_db.activation_tokens)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK, default `gen_random_uuid()` | Token kayıt kimliği |
| `user_id` | `UUID` | NOT NULL, FK → users(id) ON DELETE CASCADE | İlişkili kullanıcı |
| `token` | `VARCHAR(255)` | NOT NULL, UNIQUE | Benzersiz aktivasyon token değeri |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | Son geçerlilik tarihi (oluşturma + 24 saat) |
| `used` | `BOOLEAN` | NOT NULL, default `false` | Kullanılıp kullanılmadığı |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | Oluşturulma tarihi |

**Indexes**: `UNIQUE(token)`, `INDEX(user_id)`

**Validation Rules**:
- `token`: UUID formatında benzersiz değer
- `expires_at`: `created_at` + 24 saat
- `used`: Token kullanıldıktan sonra `true` olur, tekrar kullanılamaz
- Süresi dolmuş ve kullanılmamış tokenlar yeni aktivasyon talebinde geçersiz kabul edilir

---

### 3. Agency (identity_db.agencies)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK, default `gen_random_uuid()` | Benzersiz dükkan kimliği |
| `owner_id` | `UUID` | NOT NULL, FK → users(id) ON DELETE CASCADE | Dükkan sahibi kullanıcı |
| `name` | `VARCHAR(200)` | NOT NULL | Dükkan adı |
| `city` | `VARCHAR(100)` | NOT NULL | Şehir |
| `district` | `VARCHAR(100)` | NULL | İlçe (opsiyonel) |
| `phone` | `VARCHAR(20)` | NOT NULL | Telefon numarası |
| `contact_person` | `VARCHAR(200)` | NOT NULL | Sorumlu kişi adı-soyadı |
| `address` | `TEXT` | NOT NULL | Tam açık adres |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | Oluşturma tarihi |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | Son güncelleme tarihi |

**Indexes**: `INDEX(owner_id)`

**Validation Rules**:
- `name`: Min 2, max 200 karakter
- `city`: Min 2, max 100 karakter
- `phone`: Türkiye telefon formatı (ör: `05XX XXX XX XX`), min 10 rakam
- `contact_person`: Min 2, max 200 karakter
- `address`: Min 10, max 500 karakter

**Relationships**: Bir `User` birden fazla `Agency`'e sahip olabilir (1:N).

---

### 4. Vehicle (fleet_db.vehicles)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK, default `gen_random_uuid()` | Benzersiz araç kimliği |
| `agency_id` | `UUID` | NOT NULL | Aracın bağlı olduğu dükkan (FK değil — farklı DB) |
| `plate` | `VARCHAR(15)` | NOT NULL | Plaka (agency_id ile birlikte benzersiz) |
| `brand` | `VARCHAR(50)` | NOT NULL | Marka (ör: Toyota, BMW) |
| `model` | `VARCHAR(50)` | NOT NULL | Model (ör: Corolla, 320i) |
| `model_year` | `INTEGER` | NOT NULL | Model yılı |
| `fuel_type` | `VARCHAR(20)` | NOT NULL | Yakıt tipi: `GASOLINE`, `DIESEL`, `ELECTRIC`, `HYBRID` |
| `transmission` | `VARCHAR(20)` | NOT NULL | Vites tipi: `MANUAL`, `AUTOMATIC` |
| `daily_pool_price` | `DECIMAL(10,2)` | NOT NULL | Günlük kira/havuz bedeli (TRY) |
| `status` | `VARCHAR(20)` | NOT NULL, default `'AVAILABLE'` | Araç durumu: `AVAILABLE`, `RENTED`, `IN_POOL` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | Oluşturma tarihi |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | Son güncelleme tarihi |

**Indexes**: `UNIQUE(agency_id, plate)`, `INDEX(agency_id)`, `INDEX(status)`

**Validation Rules**:
- `plate`: Türkiye plaka formatı, max 15 karakter
- `brand`, `model`: Min 1, max 50 karakter
- `model_year`: 1980–(geçerli yıl + 1) aralığı
- `fuel_type`: Yalnızca `GASOLINE`, `DIESEL`, `ELECTRIC`, `HYBRID`
- `transmission`: Yalnızca `MANUAL`, `AUTOMATIC`
- `daily_pool_price`: > 0, max 2 ondalık basamak
- Aynı `agency_id` altında aynı `plate` eklenemez

**State Transitions**:
```text
         ┌──────────────────────────────────┐
         │                                  │
         ▼                                  │
    AVAILABLE ◄─────► RENTED                │
         │                │                 │
         │                │                 │
         ▼                ▼                 │
       IN_POOL ──────────────────────────────┘
```
Tüm durumlar arasında çift yönlü geçiş serbesttir.

---

### 5. VehicleInspection (fleet_db.vehicle_inspections)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK, default `gen_random_uuid()` | Kayıt kimliği |
| `vehicle_id` | `UUID` | NOT NULL, FK → vehicles(id) ON DELETE CASCADE, UNIQUE | İlişkili araç (1:1) |
| `traffic_insurance_expiry` | `DATE` | NULL | Trafik sigortası bitiş tarihi |
| `casco_expiry` | `DATE` | NULL | Kasko bitiş tarihi |
| `inspection_expiry` | `DATE` | NULL | Araç muayene geçerlilik tarihi |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | Son güncelleme tarihi |

**Indexes**: `UNIQUE(vehicle_id)`

**Validation Rules**:
- Tarih alanları nullable'dır (araç ilk eklendiğinde henüz girilmemiş olabilir)
- Girilen tarihler geçerli ISO tarih formatında olmalıdır
- Geçmiş tarihler kabul edilir (süresi geçmiş anlamına gelir)

**Relationships**: Bir `Vehicle`'a ait tek `VehicleInspection` kaydı (1:1). Araç silindiğinde
cascade ile silinir.

**Compliance Badge Logic** (Frontend hesaplaması):
- `days_remaining = expiry_date - today`
- `days_remaining < 0` → 🔴 **EXPIRED** (kırmızı badge)
- `0 ≤ days_remaining ≤ 30` → 🟡 **EXPIRING SOON** (sarı badge)
- `days_remaining > 30` → 🟢 **VALID** (yeşil badge / badge yok)

---

### 6. PoolListing (pool_db.pool_listings)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK, default `gen_random_uuid()` | Kayıt kimliği |
| `vehicle_id` | `UUID` | NOT NULL, UNIQUE | Kaynak araç kimliği (fleet-service'teki araç) |
| `agency_id` | `UUID` | NOT NULL | Aracın bağlı olduğu dükkan |
| `agency_name` | `VARCHAR(200)` | NOT NULL | Dükkan adı (denormalize) |
| `agency_city` | `VARCHAR(100)` | NOT NULL | Dükkan şehri (denormalize) |
| `agency_phone` | `VARCHAR(20)` | NOT NULL | Dükkan telefonu (denormalize) |
| `contact_person` | `VARCHAR(200)` | NOT NULL | Sorumlu kişi (denormalize) |
| `agency_address` | `TEXT` | NOT NULL | Dükkan adresi (denormalize) |
| `plate` | `VARCHAR(15)` | NOT NULL | Araç plakası (denormalize) |
| `brand` | `VARCHAR(50)` | NOT NULL | Marka (denormalize) |
| `model` | `VARCHAR(50)` | NOT NULL | Model (denormalize) |
| `model_year` | `INTEGER` | NOT NULL | Model yılı (denormalize) |
| `fuel_type` | `VARCHAR(20)` | NOT NULL | Yakıt tipi (denormalize) |
| `transmission` | `VARCHAR(20)` | NOT NULL | Vites tipi (denormalize) |
| `daily_pool_price` | `DECIMAL(10,2)` | NOT NULL | Günlük bedel (denormalize) |
| `listed_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | Havuza eklenme tarihi |

**Indexes**: `UNIQUE(vehicle_id)`, `INDEX(agency_id)`, `INDEX(agency_city)`, `INDEX(fuel_type)`,
`INDEX(transmission)`, `INDEX(brand)`

**Note**: Bu tablo tamamen denormalize edilmiş, read-optimized bir projection tablosudur.
Veriler Kafka event'leri aracılığıyla güncellenir — doğrudan API üzerinden yazılmaz.

---

### 7. AgencyProjection (pool_db.agency_projections)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `agency_id` | `UUID` | PK | Dükkan kimliği (`identity_db.agencies.id` karşılığı) |
| `name` | `VARCHAR(200)` | NOT NULL | Dükkan adı |
| `city` | `VARCHAR(100)` | NOT NULL | Dükkan şehri |
| `district` | `VARCHAR(100)` | NULL | Dükkan ilçesi (opsiyonel) |
| `phone` | `VARCHAR(20)` | NOT NULL | İletişim telefonu |
| `contact_person` | `VARCHAR(200)` | NOT NULL | Sorumlu kişi |
| `address` | `TEXT` | NOT NULL | Açık adres |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | Projeksiyon güncelleme zamanı |

**Indexes**: `INDEX(city)`

**Purpose**: `pool-service`'in araç havuza açıldığında (`fleet.vehicle.status-changed` event'i ile) dükkan iletişim bilgilerini yerel olarak denormalize edebilmesini sağlayan asenkron projeksiyon tablosudur (`identity.agency.upserted` eventleri ile beslenir). Böylece servisler arası doğrudan veritabanı sorgusu veya senkron HTTP bağımlılığı oluşmaz.

---

### 8. ProcessedEvents (pool_db.processed_events, fleet_db.processed_events)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `event_id` | `UUID` | PK | İşlenen event'in benzersiz kimliği |
| `topic` | `VARCHAR(100)` | NOT NULL | Kafka topic adı |
| `processed_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` | İşlenme zamanı |

**Purpose**: Kafka consumer idempotency tablosu. Aynı `event_id` ile gelen mesaj zaten tabloda
varsa işlem atlanır (dedup). Zamanla büyümesini engellemek için 30 günden eski kayıtlar
periyodik olarak temizlenebilir.

---

## Entity Relationship Summary

```text
identity_db                          fleet_db                     pool_db
┌──────────┐                        ┌───────────┐               ┌────────────────────┐
│  users   │ 1 ─── N ┌──────────┐  │ vehicles  │ 1:1           │ agency_projections │
│          │─────────►│ agencies │  │           │───┐           │ (local replica)    │
│          │         └──────────┘  │           │   │           └─────────┬──────────┘
│          │ 1 ─── N              │           │   ▼                     │
│          │─────────►┌──────────┐ └───────────┘ ┌────────────┐         │ joins on
│          │         │activation│               │ vehicle_   │         ▼ vehicle_id
│          │         │_tokens   │               │inspections │  ┌──────────────┐
│          │         └──────────┘               └────────────┘  │ pool_listings│
└──────────┘                                                    └──────────────┘

Cross-service data flow (Kafka events, NOT direct DB access):
  identity.agency.upserted ───────────────────────────────────► agency_projections & pool_listings
  fleet.vehicle.status-changed ────────────────────────────────► pool_listings (enriched via agency_projections)
```

---

## Flyway Migration Naming Convention

```text
identity-agency-service/src/main/resources/db/migration/
  V1__create_users_table.sql
  V2__create_activation_tokens_table.sql
  V3__create_agencies_table.sql

fleet-service/src/main/resources/db/migration/
  V1__create_vehicles_table.sql
  V2__create_vehicle_inspections_table.sql
  V3__create_processed_events_table.sql

pool-service/src/main/resources/db/migration/
  V1__create_agency_projections_table.sql
  V2__create_pool_listings_table.sql
  V3__create_processed_events_table.sql
```

