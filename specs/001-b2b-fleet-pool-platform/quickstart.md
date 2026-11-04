# Quickstart Validation Guide: B2B Rent-A-Car Platform

**Phase**: 1 — Design & Contracts
**Date**: 2026-11-04
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

---

## Prerequisites

- **Java 21** (JDK)
- **Node.js 20+** & **npm/pnpm**
- **Docker** & **Docker Compose** (PostgreSQL, Kafka, MailHog çalıştırmak için)
- **Git**

---

## 1. Infrastructure Setup

Docker Compose ile altyapı servisleri (PostgreSQL x3, Kafka KRaft, MailHog) ayağa kaldırılır.

```bash
cd docker/
docker compose up -d
```

**Beklenen Sonuç**:
- PostgreSQL: `localhost:5432` — `identity_db`, `fleet_db`, `pool_db` veritabanları oluşturulmuş
- Kafka: `localhost:9092` — KRaft modunda broker çalışıyor
- MailHog: `localhost:8025` (web UI), `localhost:1025` (SMTP)

**Doğrulama**:
```bash
docker compose ps
# Tüm container'lar "Up" durumunda olmalı

# PostgreSQL bağlantı testi
docker compose exec postgres psql -U rentacar -d identity_db -c "SELECT 1;"

# Kafka broker kontrolü
docker compose exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

---

## 2. Backend Services Startup

Gradle multi-project build ve servis başlatma.

```bash
cd backend/

# Build (tüm modüller)
./gradlew clean build

# Flyway migrasyonları her servis başladığında otomatik çalışır (Spring Boot auto-config)

# Servisleri sırasıyla başlatma (ayrı terminaller):
./gradlew :services:identity-agency-service:bootRun
./gradlew :services:fleet-service:bootRun
./gradlew :services:pool-service:bootRun
./gradlew :services:notification-service:bootRun
./gradlew :services:api-gateway:bootRun
```

**Beklenen Port'lar**:
| Service | Port |
|---------|------|
| api-gateway | 8080 |
| identity-agency-service | 8081 |
| fleet-service | 8082 |
| pool-service | 8083 |
| notification-service | 8084 |

**Doğrulama**: Her servisin health endpoint'ine erişim:
```bash
curl http://localhost:8080/actuator/health   # Gateway
curl http://localhost:8081/actuator/health   # Identity
curl http://localhost:8082/actuator/health   # Fleet
curl http://localhost:8083/actuator/health   # Pool
curl http://localhost:8084/actuator/health   # Notification
```

Her yanıtta `{"status":"UP"}` dönmelidir.

---

## 3. Frontend Startup

```bash
cd frontend/

npm install   # veya pnpm install
npm run dev   # Vite dev server
```

**Beklenen Sonuç**: `http://localhost:5173` adresinde SPA çalışıyor.

---

## 4. End-to-End Validation Scenarios

### Scenario 1: Kullanıcı Kayıt ve Aktivasyon (User Stories 1)

**Adımlar**:
1. Frontend'te `/register` sayfasına git
2. Form doldur: Ad, Soyad, E-posta, Şifre
3. Kayıt butonuna tıkla

**Beklenen Sonuçlar**:
- API: `POST /api/auth/register` → 201 Created
- Kafka: `identity.user.registered` event'i üretildi
- MailHog: `http://localhost:8025` adresinde aktivasyon e-postası görünür
- E-postadaki bağlantı: `http://localhost:5173/activate?token=<uuid>`

4. MailHog'daki aktivasyon bağlantısına tıkla (veya tarayıcıda aç)

**Beklenen Sonuçlar**:
- API: `POST /api/auth/activate` → 200 OK, `status: ACTIVE`
- Giriş sayfasına yönlendirme

5. Aktive edilmiş hesapla giriş yap

**Beklenen Sonuçlar**:
- API: `POST /api/auth/login` → 200 OK, JWT token döner
- Frontend: Dashboard'a yönlendirme

**Negatif Test**: PENDING statüsündeki hesapla giriş dene → 403 Forbidden

---

### Scenario 2: Çoklu Dükkan Yönetimi (User Story 2)

**Ön Koşul**: Aktif hesap ile giriş yapılmış

**Adımlar**:
1. Dükkan ekleme formuna git
2. 2 adet dükkan ekle (farklı şehirler)

**Beklenen Sonuçlar**:
- API: `POST /api/agencies` → 201 Created (her biri için)
- Üst barda dükkan seçici (Agency Switcher) görünür, 2 dükkan listelenir
- Dükkanlar arası geçiş yapıldığında bağlam değişir

3. Bir dükkanın bilgilerini güncelle

**Beklenen Sonuçlar**:
- API: `PUT /api/agencies/{id}` → 200 OK
- Kafka: `identity.agency.upserted` event'i üretildi

---

### Scenario 3: Araç Ekleme ve Sigorta/Muayene Takibi (User Story 3)

**Ön Koşul**: En az 1 dükkan oluşturulmuş

**Adımlar**:
1. Aktif dükkanı seç
2. "Araç Ekle" formunu aç
3. Araç bilgilerini gir:
   - Marka: Toyota, Model: Corolla, Yıl: 2024
   - Plaka: 34 ABC 123
   - Yakıt: Benzin, Vites: Otomatik, Günlük: 1500 TL
   - Trafik Sigortası: (bugünden 15 gün sonra), Kasko: (6 ay sonra), Muayene: (geçmiş bir tarih)

**Beklenen Sonuçlar**:
- API: `POST /api/vehicles` → 201 Created
- Filo listesinde araç görünür
- Trafik sigortası: 🟡 EXPIRING SOON badge'i (15 gün kalmış)
- Muayene: 🔴 EXPIRED badge'i (geçmiş tarih)
- Kasko: Badge yok veya 🟢 (6 ay var)

**Negatif Test**: Aynı plakayı aynı dükkana tekrar eklemeyi dene → 409 Conflict

---

### Scenario 4: Araç Durumu Değiştirme ve Havuz Yansıması (User Stories 4 & 5)

**Ön Koşul**: En az 1 araç eklenmiş, 2 farklı kullanıcı hesabı mevcut

**Adımlar**:
1. Kullanıcı A: Bir aracın durumunu "Havuzda" (IN_POOL) olarak değiştir

**Beklenen Sonuçlar**:
- API: `PATCH /api/vehicles/{id}/status` → 200 OK, `status: IN_POOL`
- Kafka: `fleet.vehicle.status-changed` event'i (`newStatus: IN_POOL`)
- Pool DB: `pool_listings` tablosuna kayıt eklendi

2. Kullanıcı B: Ortak Havuz (Pool) sayfasına git

**Beklenen Sonuçlar**:
- API: `GET /api/pool/listings` → 200 OK, Kullanıcı A'nın aracı listede
- Araç kartında marka, model, yıl, günlük bedel görünür
- Dükkan iletişim kartına tıklandığında ad, telefon, sorumlu kişi, adres görünür

3. Kullanıcı A: Aracın durumunu "Uygun" (AVAILABLE) olarak geri değiştir

**Beklenen Sonuçlar**:
- Kafka: `fleet.vehicle.status-changed` event'i (`newStatus: AVAILABLE`)
- Kullanıcı B'nin havuz ekranında araç artık görünmez (sayfa yenilemesi sonrası en geç 5s)

---

### Scenario 5: Havuz Filtreleme (User Story 5)

**Ön Koşul**: Havuzda farklı özelliklerde 3+ araç mevcut

**Adımlar**:
1. Ortak Havuz sayfasını aç
2. Şehir filtresini "İstanbul" olarak seç
3. Yakıt tipi filtresini "Dizel" olarak seç

**Beklenen Sonuçlar**:
- API: `GET /api/pool/listings?city=İstanbul&fuelType=DIESEL` → yalnızca eşleşen araçlar
- Sonuç listesi 2 saniye altında yüklenir (SC-005)

---

## 5. Integration Test Validation

```bash
cd backend/

# Testcontainers tabanlı entegrasyon testleri (PostgreSQL + Kafka container'ları otomatik başlatılır)
./gradlew :services:identity-agency-service:test
./gradlew :services:fleet-service:test
./gradlew :services:pool-service:test
./gradlew :services:notification-service:test
```

**Beklenen Sonuç**: Tüm testler başarıyla geçer. Testcontainers PostgreSQL ve Kafka
container'larını otomatik olarak başlatır ve testten sonra temizler.

---

## 6. Structured Logging & Trace ID Verification

```bash
# Bir API isteği gönder ve logları kontrol et
curl -v http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","email":"test@example.com","password":"Test1234"}'

# Gateway loglarında JSON formatında traceId ve spanId bulunmalı
docker logs <gateway-container> | jq '.traceId'
# identity-agency-service loglarında aynı traceId
docker logs <identity-container> | jq '.traceId'
```

**Beklenen Sonuç**: Aynı `traceId` gateway ve identity-service loglarında yer alır. Kafka
consumer loglarında da aynı `correlationId` ile event işlendiği görülür.

