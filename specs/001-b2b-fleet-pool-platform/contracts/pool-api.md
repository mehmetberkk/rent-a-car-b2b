# API Contract: Pool Service

**Base Path**: `/api` (Gateway route prefix: `/api/pool/**`)
**Content-Type**: `application/json`
**Error Format**: `application/problem+json` (RFC 7807)
**Authentication**: JWT gerektirir. Yalnızca `ACTIVE` statüsündeki kullanıcılar erişebilir.
**Note**: Pool service read-only bir API sunar. Veriler Kafka event'leri ile güncellenir.

---

## Pool Listing Endpoints

### GET /api/pool/listings?page={n}&size={n}&city={str}&brand={str}&fuelType={enum}&transmission={enum}

Ortak havuzdaki araçları filtreli ve sayfalanmış olarak listeler. İstekte bulunan kullanıcının kendi dükkanlarına ait araçlar sonuçlardan hariç tutulur.

**Query Parameters**:
| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `page` | int | No | 0 | Sayfa numarası (0-based) |
| `size` | int | No | 20 | Sayfa başına kayıt (max 100) |
| `city` | string | No | — | Şehre göre filtre |
| `brand` | string | No | — | Markaya göre filtre |
| `fuelType` | enum | No | — | `GASOLINE`, `DIESEL`, `ELECTRIC`, `HYBRID` |
| `transmission` | enum | No | — | `MANUAL`, `AUTOMATIC` |
| `minPrice` | decimal | No | — | Minimum günlük bedel |
| `maxPrice` | decimal | No | — | Maksimum günlük bedel |
| `sort` | string | No | `listedAt,desc` | Sıralama: `dailyPoolPrice,asc`, `dailyPoolPrice,desc`, `listedAt,desc`, `modelYear,desc` |

**Headers**: `X-User-Id` ve `X-Agency-Ids` (gateway tarafından iletilir)

**Response 200 OK**:
```json
{
  "content": [
    {
      "vehicleId": "uuid",
      "plate": "34 ABC 123",
      "brand": "Toyota",
      "model": "Corolla",
      "modelYear": 2024,
      "fuelType": "GASOLINE",
      "transmission": "AUTOMATIC",
      "dailyPoolPrice": 1500.00,
      "listedAt": "ISO-8601",
      "agency": {
        "id": "uuid",
        "name": "Öz Yıldız Rent a Car",
        "city": "İstanbul",
        "phone": "0532 XXX XX XX",
        "contactPerson": "Ahmet Yılmaz",
        "address": "Kadıköy, İstanbul"
      }
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 87,
  "totalPages": 5
}
```

**Error Responses**:
- `401 Unauthorized`: JWT eksik veya geçersiz
- `403 Forbidden`: Kullanıcı hesabı aktif değil

**Behavior Notes**:
- `X-Agency-Ids` header'ındaki agency ID'lere ait araçlar sonuçlardan otomatik olarak filtrelenir
- Boş filtre parametreleri tüm kayıtları döner (filtre uygulanmaz)
- Birden fazla filtre AND mantığı ile çalışır

---

### GET /api/pool/listings/{vehicleId}

Havuzdaki tek araç detayı ve bağlı dükkanın iletişim kartı.

**Response 200 OK**:
```json
{
  "vehicleId": "uuid",
  "plate": "34 ABC 123",
  "brand": "Toyota",
  "model": "Corolla",
  "modelYear": 2024,
  "fuelType": "GASOLINE",
  "transmission": "AUTOMATIC",
  "dailyPoolPrice": 1500.00,
  "listedAt": "ISO-8601",
  "agency": {
    "id": "uuid",
    "name": "Öz Yıldız Rent a Car",
    "city": "İstanbul",
    "phone": "0532 XXX XX XX",
    "contactPerson": "Ahmet Yılmaz",
    "address": "Mecidiyeköy Mah. Büyükdere Cad. No:12, Şişli, İstanbul"
  }
}
```

**Error Responses**:
- `404 Not Found`: Araç havuzda bulunamadı (silinmiş veya durumu değiştirilmiş olabilir)

---

### GET /api/pool/filters

Havuzdaki mevcut araçlardan dinamik filtre seçeneklerini döner (aktif değerler).

**Response 200 OK**:
```json
{
  "cities": ["İstanbul", "Ankara", "İzmir", "Antalya"],
  "brands": ["Toyota", "BMW", "Volkswagen", "Renault"],
  "fuelTypes": ["GASOLINE", "DIESEL", "ELECTRIC", "HYBRID"],
  "transmissions": ["MANUAL", "AUTOMATIC"],
  "priceRange": {
    "min": 500.00,
    "max": 5000.00
  }
}
```

**Note**: Bu endpoint havuzda gerçekten mevcut olan değerleri döner; boş havuzda boş diziler döner.

