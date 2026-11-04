# API Contract: Fleet Service

**Base Path**: `/api` (Gateway route prefix: `/api/vehicles/**`)
**Content-Type**: `application/json`
**Error Format**: `application/problem+json` (RFC 7807)
**Authentication**: Tüm endpoint'ler JWT gerektirir. Gateway `X-User-Id` ve `X-Agency-Ids` iletir.
**Ownership Check**: `agencyId` parametresi veya request body'deki `agencyId` değeri, `X-Agency-Ids` header'ında mevcut olmalıdır.

---

## Vehicle CRUD Endpoints

### POST /api/vehicles

Seçili dükkana yeni araç ekler.

**Headers**: `Authorization: Bearer <jwt>`

**Request Body**:
```json
{
  "agencyId": "uuid (required, must be in user's agencyIds)",
  "plate": "string (required, max 15 chars)",
  "brand": "string (1-50 chars, required)",
  "model": "string (1-50 chars, required)",
  "modelYear": "integer (1980 - current_year+1, required)",
  "fuelType": "GASOLINE | DIESEL | ELECTRIC | HYBRID (required)",
  "transmission": "MANUAL | AUTOMATIC (required)",
  "dailyPoolPrice": "decimal (> 0, max 2 decimal places, required)",
  "trafficInsuranceExpiry": "date (ISO-8601, optional)",
  "cascoExpiry": "date (ISO-8601, optional)",
  "inspectionExpiry": "date (ISO-8601, optional)"
}
```

**Response 201 Created**:
```json
{
  "id": "uuid",
  "agencyId": "uuid",
  "plate": "string",
  "brand": "string",
  "model": "string",
  "modelYear": 2024,
  "fuelType": "GASOLINE",
  "transmission": "AUTOMATIC",
  "dailyPoolPrice": 1500.00,
  "status": "AVAILABLE",
  "trafficInsuranceExpiry": "2026-12-31",
  "cascoExpiry": "2027-03-15",
  "inspectionExpiry": "2027-06-01",
  "createdAt": "ISO-8601"
}
```

**Error Responses**:
- `400 Bad Request`: Validasyon hatası
- `403 Forbidden`: `agencyId` kullanıcının dükkanlarından biri değil
- `409 Conflict`: Aynı `agencyId` + `plate` kombinasyonu zaten mevcut

---

### GET /api/vehicles?agencyId={uuid}&page={n}&size={n}

Belirtilen dükkanın araçlarını sayfalanmış olarak listeler.

**Query Parameters**:
| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `agencyId` | UUID | Yes | — | Araçları listelenen dükkan |
| `page` | int | No | 0 | Sayfa numarası (0-based) |
| `size` | int | No | 20 | Sayfa başına kayıt (max 100) |

**Response 200 OK**:
```json
{
  "content": [
    {
      "id": "uuid",
      "agencyId": "uuid",
      "plate": "34 ABC 123",
      "brand": "Toyota",
      "model": "Corolla",
      "modelYear": 2024,
      "fuelType": "GASOLINE",
      "transmission": "AUTOMATIC",
      "dailyPoolPrice": 1500.00,
      "status": "AVAILABLE",
      "trafficInsuranceExpiry": "2026-12-31",
      "cascoExpiry": "2027-03-15",
      "inspectionExpiry": "2027-06-01"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

### GET /api/vehicles/{vehicleId}

Tek araç detayı.

**Response 200 OK**: Tek vehicle nesnesi (yukarıdaki formatta).

**Error Responses**:
- `403 Forbidden`: Araç kullanıcının dükkanına ait değil
- `404 Not Found`: Araç bulunamadı

---

### PUT /api/vehicles/{vehicleId}

Araç bilgilerini günceller. Güncelleme sonrası eğer araç `IN_POOL` statüsündeyse `fleet.vehicle.status-changed` event'i fırlatılır (havuz bilgilerinin güncellenmesi için).

**Request Body**: POST ile aynı yapı (`agencyId` hariç — değiştirilemez).

**Response 200 OK**: Güncellenmiş vehicle nesnesi.

**Error Responses**:
- `400 Bad Request`: Validasyon hatası
- `403 Forbidden`: Araç kullanıcının dükkanına ait değil
- `404 Not Found`: Araç bulunamadı
- `409 Conflict`: Plaka çakışması

---

### DELETE /api/vehicles/{vehicleId}

Aracı siler. Eğer araç `IN_POOL` ise silerken `fleet.vehicle.status-changed` event'i (durum `AVAILABLE` veya özel `DELETED` flag) fırlatılarak pool-service'in havuzdan kaldırmasını sağlar.

**Response 204 No Content**

**Error Responses**:
- `403 Forbidden`: Araç kullanıcının dükkanına ait değil
- `404 Not Found`: Araç bulunamadı

---

## Vehicle Status Endpoint

### PATCH /api/vehicles/{vehicleId}/status

Aracın durumunu değiştirir. Başarılı güncelleme sonrası `fleet.vehicle.status-changed` Kafka event'i fırlatılır.

**Request Body**:
```json
{
  "status": "AVAILABLE | RENTED | IN_POOL (required)"
}
```

**Response 200 OK**:
```json
{
  "id": "uuid",
  "plate": "string",
  "status": "IN_POOL",
  "previousStatus": "AVAILABLE",
  "updatedAt": "ISO-8601"
}
```

**Error Responses**:
- `400 Bad Request`: Geçersiz durum değeri
- `403 Forbidden`: Araç kullanıcının dükkanına ait değil
- `404 Not Found`: Araç bulunamadı

