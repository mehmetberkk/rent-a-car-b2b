# Kafka Event Contracts

**Broker**: Apache Kafka (KRaft mode, single broker dev)
**Serialization**: JSON
**Trace Context**: W3C `traceparent` header taşınır (Micrometer Tracing)
**Idempotency**: Tüm consumer'lar `eventId` bazlı dedup uygular
**Error Handling**: 3 retry (exponential backoff: 1s, 2s, 4s) + `<topic>.DLT`

---

## Event Envelope (Ortak Zarf Yapısı)

Tüm Kafka event'leri `common-events` modülünde tanımlı aşağıdaki zarf yapısını kullanır:

```json
{
  "eventId": "uuid (benzersiz, idempotency key)",
  "eventType": "string (ör: identity.user.registered)",
  "occurredOn": "ISO-8601 (event oluşma zamanı)",
  "correlationId": "uuid (trace/correlation ID, istek boyunca taşınır)",
  "payload": { }
}
```

---

## Topic: identity.user.registered

**Producer**: `identity-agency-service`
**Consumer(s)**: `notification-service`
**Trigger**: Yeni kullanıcı kayıt olduğunda

**Payload**:
```json
{
  "userId": "uuid",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "activationToken": "string (UUID token değeri)"
}
```

**Consumer Action (notification-service)**:
Aktivasyon e-postası gönderir. İçerik:
`{FRONTEND_URL}/activate?token={activationToken}` bağlantısını içerir.

**Idempotency**: `eventId` ile dedup. Aynı kayıt event'i tekrar gelirse
e-posta ikinci kez gönderilmez.

---

## Topic: identity.agency.upserted

**Producer**: `identity-agency-service`
**Consumer(s)**: `pool-service`
**Trigger**: Bir dükkan yeni oluşturulduğunda (`POST /api/agencies`) veya iletişim/adres bilgileri güncellendiğinde (`PUT /api/agencies/{id}`)

**Payload**:
```json
{
  "agencyId": "uuid",
  "name": "string",
  "city": "string",
  "district": "string | null",
  "phone": "string",
  "contactPerson": "string",
  "address": "string"
}
```

**Consumer Action (pool-service)**:
1. `agency_projections` tablosuna UPSERT yapar (`agency_id = payload.agencyId`).
2. Eğer bu dükkana ait havuzda araç varsa (`pool_listings` tablosunda `agency_id = payload.agencyId`), bu kayıtların denormalize dükkan bilgilerini (`agency_name`, `agency_city`, `agency_phone`, `contact_person`, `agency_address`) günceller.

**Idempotency**: `eventId` ile dedup (`processed_events` tablosu). UPSERT işlemi doğal olarak idempotent'tir.

---

## Topic: fleet.vehicle.status-changed

**Producer**: `fleet-service`
**Consumer(s)**: `pool-service`
**Trigger**: Araç durumu değiştiğinde, araç bilgileri güncellendiğinde (ve araç `IN_POOL` ise)
veya araç silindiğinde

**Payload**:
```json
{
  "vehicleId": "uuid",
  "agencyId": "uuid",
  "plate": "string",
  "brand": "string",
  "model": "string",
  "modelYear": 2024,
  "fuelType": "GASOLINE",
  "transmission": "AUTOMATIC",
  "dailyPoolPrice": 1500.00,
  "newStatus": "IN_POOL | AVAILABLE | RENTED | DELETED",
  "previousStatus": "AVAILABLE | RENTED | IN_POOL | null"
}
```

**Consumer Action (pool-service)**:
- `newStatus = IN_POOL` → `pool-service` yerel `agency_projections` tablosundan `agency_id = payload.agencyId` kaydını okur. Araç bilgileri (payload) ve dükkan iletişim bilgileri (yerel projeksiyon) birleştirilerek `pool_listings` tablosuna UPSERT yapılır (`vehicle_id` bazında). Böylece `fleet-service` dükkan iletişim detaylarını taşımak veya `identity_db`'ye erişmek zorunda kalmaz.
- `newStatus = AVAILABLE | RENTED | DELETED` → `pool_listings` tablosundan `vehicle_id` ile eşleşen kaydı DELETE eder.

**Idempotency**: `eventId` ile dedup (`processed_events` tablosu). UPSERT ve DELETE doğası gereği idempotent operasyonlardır.

---

## Topic: fleet.vehicle.compliance-alert

**Producer**: `fleet-service` (günlük scheduled job)
**Consumer(s)**: `notification-service`
**Trigger**: Günlük cron job sigorta/muayene tarihlerini kontrol eder; 30 gün içinde dolacak
veya süresi geçmiş araçlar için fırlatılır

**Payload**:
```json
{
  "vehicleId": "uuid",
  "agencyId": "uuid",
  "plate": "string",
  "brand": "string",
  "model": "string",
  "ownerUserId": "uuid",
  "ownerEmail": "string",
  "alerts": [
    {
      "type": "TRAFFIC_INSURANCE | CASCO | INSPECTION",
      "expiryDate": "date (ISO-8601)",
      "daysRemaining": -5,
      "severity": "EXPIRED | EXPIRING_SOON"
    }
  ]
}
```

**Consumer Action (notification-service)**:
Araç sahibi kullanıcıya (`ownerEmail`) uyarı e-postası gönderir.
E-posta içeriğinde araç plakası, uyarı tipi ve bitiş tarihi yer alır.

**Idempotency**: `eventId` ile dedup. Aynı gün içinde aynı araç-uyarı kombinasyonu için
tekrar e-posta gönderilmez.

---

## Dead Letter Topics

| Source Topic | DLT Topic | Purpose |
|---|---|---|
| `identity.user.registered` | `identity.user.registered.DLT` | Aktivasyon e-postası gönderilemedi |
| `identity.agency.upserted` | `identity.agency.upserted.DLT` | Havuz dükkan projeksiyonu upsert başarısız |
| `fleet.vehicle.status-changed` | `fleet.vehicle.status-changed.DLT` | Havuz kayıt upsert/delete başarısız |
| `fleet.vehicle.compliance-alert` | `fleet.vehicle.compliance-alert.DLT` | Compliance uyarı e-postası gönderilemedi |

---

## Consumer → Topic Mapping

| Service | Consumes | Produces |
|---------|----------|----------|
| `identity-agency-service` | — | `identity.user.registered`, `identity.agency.upserted` |
| `fleet-service` | — | `fleet.vehicle.status-changed`, `fleet.vehicle.compliance-alert` |
| `pool-service` | `fleet.vehicle.status-changed`, `identity.agency.upserted` | — |
| `notification-service` | `identity.user.registered`, `fleet.vehicle.compliance-alert` | — |

