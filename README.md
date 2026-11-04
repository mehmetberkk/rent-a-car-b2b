# B2B Rent A Car Filo Yönetimi ve Ortak Araç Havuzu Platformu

Rent a car dükkan sahiplerinin (Agency Owner) birden fazla şubesini tek çatı altında yönetebildiği, araç filosunun operasyonel süreçlerini (muayene, kasko, trafik sigortası) takip edebildiği ve boşta duran araçlarını diğer dükkan sahipleriyle paylaşıma açabildiği B2B filo ve ortak pazar yeri platformudur.

---

## 🌟 Temel Özellikler

1. **Kullanıcı Yönetimi ve E-Posta Aktivasyonu (User Story 1)**
   - Dükkan sahipleri sisteme kaydolur, hesapları `PENDING` statüsünde oluşturulur.
   - Kafka üzerinden asenkron tetiklenen aktivasyon e-postasındaki jetona tıklanarak hesap `ACTIVE` edilir.
   - Aktif olmayan hesapların sisteme giriş yapması engellenir.

2. **Çoklu Dükkan (Multi-Agency) Yönetimi (User Story 2)**
   - Tek bir kullanıcı hesabı üzerinden birden fazla şube / acente kaydı ve yönetimi.
   - Dükkan eklendiğinde/silindiğinde anında güncellenen JWT yetkilendirmesi (`refreshedToken` & `X-Agency-Ids`).
   - Arayüzde üst bar üzerinden anlık dükkan değiştirme (Agency Switcher).

3. **Filo ve Operasyonel İzleme (User Story 3)**
   - Plaka, marka, model, yıl, yakıt, vites ve günlük havuz bedeli ile araç envanteri.
   - Trafik sigortası, kasko ve araç muayene geçerlilik sürelerinin takibi.
   - Süresi yaklaşan (≤30 gün) sarı `EXPIRING_SOON` ve süresi geçen kırmızı `EXPIRED` rozetleri.
   - Her gece çalışan scheduled cron job ile yaklaşan veya süresi geçen araçlar için otomatik e-posta uyarıları.

4. **Araç Durum Yönetimi (User Story 4)**
   - Tek tıkla statü değişimi: `Uygun` (AVAILABLE), `Dolu` (RENTED), `Havuzda` (IN_POOL).
   - Durum `IN_POOL` olduğunda Kafka `fleet.vehicle.status-changed` event'i ile `pool-service`'e anında bildirim.

5. **Ortak Araç Havuzu Pazaryeri (User Story 5)**
   - Durumu `IN_POOL` olan tüm doğrulanmış dükkan araçlarının ortak vitrinde listelenmesi.
   - Şehir, marka, yakıt tipi, vites tipi ve fiyat aralığına göre anlık filtreleme.
   - Kullanıcının kendi araçlarını tek tıkla gizleyebilme seçeneği.
   - İlgilenilen havuz aracı için dükkan adı, şehir, adres, yetkili kişi ve telefon bilgilerini gösteren iletişim kartı.

---

## 🏗️ Mimari ve Teknoloji Yığını

### Backend
- **Java 21**, **Spring Boot 3.3.x**, **Gradle Multi-Project**
- **Spring Cloud Gateway**: Stateless JWT imza doğrulaması ve `X-User-Id`, `X-User-Email`, `X-Agency-Ids` header yayılımı.
- **Microservices**:
  - `identity-agency-service` (Port: 8081): Kullanıcı kaydı, JWT üretimi, şube CRUD işlemleri.
  - `fleet-service` (Port: 8082): Araç envanteri, sigorta/muayene takibi, durum yönetimi.
  - `pool-service` (Port: 8083): Ortak havuz pazaryeri, CQRS read-projection tablosu.
  - `notification-service` (Port: 8084): Asenkron e-posta aktivasyonu ve muayene hatırlatmaları.
- **Apache Kafka (KRaft)**: Servisler arası asenkron olay akışı (`identity.user.registered`, `identity.agency.upserted`, `fleet.vehicle.status-changed`, `fleet.vehicle.compliance-alert`), DLT ve retry mekanizması.
- **PostgreSQL 16**: Database-per-service prensibi (`identity_db`, `fleet_db`, `pool_db`), Flyway şema versiyonlama.
- **Hata Yönetimi**: RFC 7807 Problem Details (`application/problem+json`).

### Frontend
- **React 18**, **TypeScript**, **Vite**
- **Tailwind CSS**: Modern ve duyarlı (responsive) kurumsal arayüz.
- **TanStack Query (React Query)**: Sunucu durumu ve veri önbellekleme (cache invalidation).
- **Axios**: JWT interceptor ve RFC 7807 hata yakalayıcı.
- **React Router v6**: Korumalı rotalar (`ProtectedRoute`) ve dükkan bağlamı.

---

## 📁 Dizin Yapısı

```
rent-a car spec/
├── backend/
│   ├── common/
│   │   ├── common-dto/          # RFC 7807 ProblemDetails, PageResponse, ApiResponse
│   │   └── common-events/       # Kafka EventEnvelope ve domain payload record'ları
│   └── services/
│       ├── api-gateway/         # Port 8080 (Spring Cloud Gateway, JwtAuthFilter)
│       ├── identity-agency-service/ # Port 8081 (Auth & Multi-Agency)
│       ├── fleet-service/       # Port 8082 (Araç Envanteri & Takip)
│       ├── pool-service/        # Port 8083 (Ortak Havuz & Projeksiyon)
│       └── notification-service/# Port 8084 (E-posta Tüketicileri)
├── frontend/
│   ├── src/
│   │   ├── agency/              # Dükkan yönetimi ve AgencySwitcher
│   │   ├── auth/                # Login, Register, Activate
│   │   ├── fleet/               # Filo listesi, Araç Modal, ComplianceBadge, StatusSwitcher
│   │   ├── pool/                # Ortak Havuz Pazaryeri, Filtreler, İletişim Kartı
│   │   └── shared/              # Navbar, Layout, Korumalı rotalar, Tipler
├── docker/
│   ├── docker-compose.yml       # PostgreSQL x3, Kafka KRaft, Mailpit
│   ├── init-databases.sql       # identity_db, fleet_db, pool_db
│   └── .env.example
└── specs/
    └── 001-b2b-fleet-pool-platform/ # Anayasa, spec, plan, tasks, quickstart
```

---

## 🚀 Hızlı Başlangıç

### 1. Altyapıyı Başlatın (Docker)

```bash
cd docker
docker compose up -d
```
- PostgreSQL: `localhost:5432`
- Kafka KRaft: `localhost:9092`
- Mailpit Web Arayüzü: `http://localhost:8025`

### 2. Backend Servislerini Başlatın

```bash
cd backend
./gradlew build

# Servisleri ayrı terminallerde başlatabilirsiniz:
./gradlew :services:identity-agency-service:bootRun
./gradlew :services:fleet-service:bootRun
./gradlew :services:pool-service:bootRun
./gradlew :services:notification-service:bootRun
./gradlew :services:api-gateway:bootRun
```

### 3. Frontend Uygulamasını Başlatın

```bash
cd frontend
npm install
npm run dev
```

Uygulamaya tarayıcınızdan `http://localhost:5173` adresinden erişebilirsiniz.

