# API Contract: Identity & Agency Service

**Base Path**: `/api` (Gateway route prefix: `/api/auth/**`, `/api/agencies/**`)
**Content-Type**: `application/json`
**Error Format**: `application/problem+json` (RFC 7807)

---

## Authentication Endpoints

### POST /api/auth/register

Yeni kullanıcı kaydı. Kayıt sonrası `identity.user.registered` Kafka event'i fırlatılır.

**Request Body**:
```json
{
  "firstName": "string (2-100 chars, required)",
  "lastName": "string (2-100 chars, required)",
  "email": "string (valid email, required)",
  "password": "string (min 8 chars, 1 uppercase, 1 lowercase, 1 digit, required)"
}
```

**Response 201 Created**:
```json
{
  "id": "uuid",
  "email": "string",
  "status": "PENDING",
  "message": "Aktivasyon e-postası gönderildi. Lütfen e-postanızı kontrol edin."
}
```

**Error Responses**:
- `409 Conflict`: E-posta zaten kayıtlı
- `400 Bad Request`: Validasyon hatası (`invalidParams` ile)

---

### POST /api/auth/activate

Aktivasyon token'ı ile hesap doğrulama.

**Request Body**:
```json
{
  "token": "string (uuid, required)"
}
```

**Response 200 OK**:
```json
{
  "message": "Hesabınız başarıyla aktive edildi.",
  "status": "ACTIVE"
}
```

**Error Responses**:
- `400 Bad Request`: Token geçersiz veya süresi dolmuş
- `409 Conflict`: Token zaten kullanılmış

---

### POST /api/auth/resend-activation

Süresi dolmuş veya ulaşmamış aktivasyon e-postası için yeni token üretir.

**Request Body**:
```json
{
  "email": "string (valid email, required)"
}
```

**Response 200 OK**:
```json
{
  "message": "Yeni aktivasyon e-postası gönderildi."
}
```

**Error Responses**:
- `404 Not Found`: E-posta kayıtlı değil
- `409 Conflict`: Hesap zaten aktif

---

### POST /api/auth/login

Kullanıcı girişi. JWT token üretir.

**Request Body**:
```json
{
  "email": "string (required)",
  "password": "string (required)"
}
```

**Response 200 OK**:
```json
{
  "accessToken": "string (JWT)",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "firstName": "string",
    "lastName": "string",
    "email": "string"
  },
  "agencyIds": ["uuid", "uuid"]
}
```

**JWT Claims (payload)**:
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "agencyIds": ["uuid-1", "uuid-2"],
  "iat": 1234567890,
  "exp": 1234571490
}
```

**Error Responses**:
- `401 Unauthorized`: E-posta veya şifre hatalı
- `403 Forbidden`: Hesap henüz aktive edilmemiş (PENDING)

---

### POST /api/auth/refresh

JWT token yenileme. Kullanıcının güncel `agencyIds` listesi yeni token'a yansır.

**Headers**: `Authorization: Bearer <current-jwt>`

**Response 200 OK**:
```json
{
  "accessToken": "string (new JWT)",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "agencyIds": ["uuid", "uuid", "uuid"]
}
```

---

## Agency Endpoints

> **Authentication**: Tüm agency endpoint'leri JWT gerektirir.
> Gateway `X-User-Id` ve `X-Agency-Ids` header'larını iletir.

### POST /api/agencies

Yeni dükkan oluşturma. Dükkan oluşturulduğunda `identity.agency.upserted` Kafka event'i yayınlanır.
Ayrıca, kullanıcının downstream servislerde (`fleet-service`) yeni dükkanı için anında yetkilendirilmesi amacıyla yanıt gövdesinde güncel `agencyIds` claim'ini içeren yeni bir `accessToken` (`refreshedToken`) döndürülür.

**Headers**: `Authorization: Bearer <jwt>`

**Request Body**:
```json
{
  "name": "string (2-200 chars, required)",
  "city": "string (2-100 chars, required)",
  "district": "string (2-100 chars, optional)",
  "phone": "string (10+ digits, required)",
  "contactPerson": "string (2-200 chars, required)",
  "address": "string (10-500 chars, required)"
}
```

**Response 201 Created**:
```json
{
  "agency": {
    "id": "uuid",
    "ownerId": "uuid",
    "name": "string",
    "city": "string",
    "district": "string | null",
    "phone": "string",
    "contactPerson": "string",
    "address": "string",
    "createdAt": "ISO-8601"
  },
  "refreshedToken": {
    "accessToken": "string (updated JWT containing new agencyId in agencyIds claim)",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "agencyIds": ["uuid-existing", "uuid-new"]
  }
}
```

---

### GET /api/agencies

Oturum açmış kullanıcının dükkanlarını listeler.

**Headers**: `Authorization: Bearer <jwt>`

**Response 200 OK**:
```json
[
  {
    "id": "uuid",
    "name": "string",
    "city": "string",
    "district": "string | null",
    "phone": "string",
    "contactPerson": "string",
    "address": "string"
  }
]
```

---

### GET /api/agencies/{agencyId}

Belirtilen dükkan detayı. İstekte bulunan kullanıcı sahiplik kontrolü yapılır (`agencyId ∈ X-Agency-Ids`).

**Response 200 OK**: Tek agency nesnesi (yukarıdaki formatta).

**Error Responses**:
- `403 Forbidden`: Kullanıcı bu dükkanın sahibi değil
- `404 Not Found`: Dükkan bulunamadı

---

### PUT /api/agencies/{agencyId}

Dükkan bilgilerini günceller. Başarılı güncelleme sonrası `identity.agency.upserted` Kafka event'i fırlatılır.

**Headers**: `Authorization: Bearer <jwt>`

**Request Body**: POST ile aynı yapı.

**Response 200 OK**: Güncellenmiş agency nesnesi.

**Error Responses**:
- `403 Forbidden`: Kullanıcı bu dükkanın sahibi değil
- `404 Not Found`: Dükkan bulunamadı

---

### DELETE /api/agencies/{agencyId}

Dükkanı siler. Silme öncesinde dükkana bağlı araç olup olmadığı doğrulanır; aktif filosu olan dükkanlar silinemez. Silme başarılı olduğunda kullanıcının güncellenmiş token'ı (`refreshedToken`) döner.

**Response 200 OK**:
```json
{
  "message": "Dükkan başarıyla silindi.",
  "refreshedToken": {
    "accessToken": "string (updated JWT without deleted agencyId)",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "agencyIds": ["uuid-remaining"]
  }
}
```

**Error Responses**:
- `403 Forbidden`: Kullanıcı bu dükkanın sahibi değil
- `404 Not Found`: Dükkan bulunamadı
- `409 Conflict`: Dükkana kayıtlı araçlar bulunmaktadır (`urn:rentacar:problem:identity:agency-has-vehicles`)

