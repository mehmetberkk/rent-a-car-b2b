# Research: B2B Rent-A-Car Filo Yönetimi ve Ortak Araç Havuzu Platformu

**Phase**: 0 — Outline & Research
**Date**: 2026-11-04
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

---

## R1: JWT Üretimi, Multi-Agency Claims ve Gateway Doğrulaması

**Decision**: `identity-agency-service` JWT üretir. JWT payload'ında `userId` (UUID), `email`
(String) ve `agencyIds` (List<UUID>) claim'leri yer alır. `api-gateway` yalnızca JWT imza
doğrulaması yapar (`HS256` symmetric key veya `RS256` asymmetric key — başlangıçta `HS256`
yeterlidir, tek signing key tüm servisler arasında paylaşılır). Gateway doğrulama sonrası
`X-User-Id` ve `X-Agency-Ids` (virgülle ayrılmış UUID listesi) header'larını downstream'e iletir.

**Rationale**: Gateway'de merkezi doğrulama, her serviste JWT kütüphanesi ve key yönetimi
gerekliliğini ortadan kaldırır. `agencyIds` listesinin JWT'ye gömülmesi dükkan sahiplik
kontrolünü (ownership check) downstream servislerde ek veritabanı sorgusu olmadan mümkün kılar.

**Alternatives Considered**:
- **RS256 asymmetric key**: Daha güvenli (private key yalnızca identity-service'te) ama key
  rotation altyapısı başlangıç aşamasında gereksiz karmaşıklık ekler. İleride geçiş mümkün.
- **Token introspection (opaque tokens)**: Her istekte identity-service'e call gerektirir,
  performans dezavantajı. Stateless prensibine aykırı.
- **agencyIds'i her istekte DB'den çekmek**: JWT'ye gömmek yerine downstream'de DB call yapılabilir
  ama latency ve DB load artırır; constitution Principle III buna gerek duymaz.

**Open Items**: `agencyIds` claim'i JWT'nin ömrü boyunca sabit kalır. Kullanıcı yeni bir dükkan
eklediğinde bir sonraki login/refresh'te güncellenecektir. Canlı oturum sırasında eklenen yeni
dükkan için token refresh endpoint'i planlanmalıdır.

---

## R2: Database-per-Service PostgreSQL Yapılandırması

**Decision**: Docker Compose'da 3 ayrı PostgreSQL container veya tek PostgreSQL instance üzerinde 3
izole veritabanı (`identity_db`, `fleet_db`, `pool_db`). Geliştirme ortamında tek PostgreSQL
instance + 3 veritabanı yeterlidir (daha az kaynak). `notification-service` durumsuz (stateless)
çalışır, kendi veritabanına ihtiyaç duymaz (e-posta gönderim logları uygulama loglarında tutulur).

**Rationale**: Constitution Principle II — database-per-service zorunlu. Tek instance üzerindeki
izole veritabanları dev ortamında yeterli izolasyonu sağlar; prodüksiyon ortamında ayrı instance'lara
geçiş konfigürasyon değişikliğidir.

**Alternatives Considered**:
- **3 ayrı PostgreSQL container**: Daha gerçekçi prod simülasyonu ama geliştirici makinesinde gereksiz
  kaynak tüketimi.
- **notification-service için ayrı DB**: E-posta gönderim loglarını saklamak için ayrı DB
  düşünülebilir ama başlangıçta kapsam dışı; structured logging yeterli.

---

## R3: Kafka KRaft Mode ve Topic Yapılandırması

**Decision**: Apache Kafka tek broker, KRaft modunda (ZooKeeper'sız). Docker Compose ile tek
`kafka` container. Topic'ler otomatik oluşturulabilir (`auto.create.topics.enable=false`, uygulama
başlangıcında `NewTopic` bean'leri ile programatik oluşturma tercih edilir).

Topic adlandırma standardı:
- `identity.user.registered` — Kullanıcı kayıt olayı
- `identity.agency.updated` — Dükkan bilgi güncelleme olayı
- `fleet.vehicle.status-changed` — Araç durum/bilgi değişikliği
- `fleet.vehicle.compliance-alert` — Sigorta/muayene uyarısı

Her topic tek partition ile başlar (başlangıç ölçeğinde yeterli).

**Rationale**: KRaft, Kafka 3.x+ ile production-ready; ZooKeeper bağımlılığını ortadan kaldırır.
Programatik topic oluşturma şema versiyonlama mantığıyla tutarlı ve tekrarlanabilir.

**Alternatives Considered**:
- **RabbitMQ**: Daha basit kurulum ama constitution Kafka'yı belirtir.
- **Manual topic creation via CLI**: Tekrarlanabilir değil, CI/CD ile uyumsuz.
- **Auto-create topics**: Typo'lardan dolayı istenmeyen topic'ler oluşabilir.

---

## R4: Kafka Consumer Idempotency ve DLT Stratejisi

**Decision**: Spring Kafka `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` kullanılır.
Her consumer idempotency'yi `eventId` (UUID) ile sağlar: işlenmiş eventId'ler ya ayrı bir
`processed_events` tablosunda saklanır (pool-service, notification-service) ya da doğal idempotent
operasyondur (upsert/delete). Retry: 3 deneme, exponential backoff (1s, 2s, 4s). Başarısız
mesajlar `<topic>.DLT` topic'ine yönlendirilir.

**Rationale**: Constitution Principle IV — idempotent consumer'lar ve DLT zorunlu. `eventId`
tabanlı dedup basit ve güvenilirdir. Exponential backoff geçici hataları (transient failures)
absorbe eder.

**Alternatives Considered**:
- **Kafka Streams exactlyOnce**: Overkill, basit consumer senaryoları için gereksiz karmaşıklık.
- **Redis-based dedup**: Ekstra bileşen; başlangıç aşamasında PostgreSQL tabanlı dedup yeterli.
- **Infinite retry**: DLT'ye hiç göndermemek tehlikeli — zehirli mesajlar (poison pills) consumer'ı
  bloke eder.

---

## R5: Ortak Havuz (Pool) Denormalizasyon Stratejisi

**Decision**: `pool-service` kendi veritabanındaki (`pool_db`) `pool_listings` tablosunu yönetir.
Bu tablo araç ve dükkan bilgilerinin denormalize edilmiş kopyasıdır. Veriler iki Kafka event ile
güncellenir:
1. `fleet.vehicle.status-changed` → Araç `IN_POOL` ise upsert, değilse delete
2. `identity.agency.updated` → İlgili `agency_id` ile eşleşen kayıtların dükkan bilgilerini güncelle

**Rationale**: Constitution Principle II — pool-service fleet-service'in veya identity-service'in
DB'sine doğrudan erişemez. Denormalize tablo, havuz ekranında tek tablo üzerinden hızlı
filtreleme/sayfalama sağlar. Eventual consistency kabul edilebilir (SC-004: ≤5s).

**Alternatives Considered**:
- **pool-service'ten fleet-service'e senkron HTTP call**: Her havuz görüntülemede cross-service
  call → latency + coupling.
- **CQRS with event sourcing**: Overengineering for current scope.
- **Materialized view**: Constitution II'ye aykırı (shared DB gerektirir).

---

## R6: E-posta Aktivasyonu ve MailHog/Mailpit Entegrasyonu

**Decision**: `notification-service`, `identity.user.registered` Kafka event'ini dinleyerek
aktivasyon e-postasını Spring Boot Mail (SMTP) ile gönderir. Lokal geliştirmede MailHog/Mailpit
SMTP sunucusu kullanılır (Docker Compose'da). Aktivasyon bağlantısı:
`{FRONTEND_URL}/activate?token={uuid}`. Frontend bu URL'i alıp `identity-agency-service`'in
`POST /api/auth/activate` endpoint'ine token'ı gönderir.

**Rationale**: E-posta gönderimleri asenkron event-driven; identity-service kayıt sırasında
senkron e-posta beklememeli. MailHog/Mailpit gerçek SMTP sunucusuna gerek kalmadan e-posta
testini mümkün kılar.

**Alternatives Considered**:
- **Identity-service'in kendi içinde senkron mail göndermesi**: Kayıt endpoint'inin latency'sini
  artırır, tek sorumluluk ilkesini ihlal eder.
- **Frontend'ten doğrudan activation token URL**: Backend doğrulaması olmadan güvensiz.

---

## R7: Frontend Durum Yönetimi ve Agency Context

**Decision**: React Context API ile iki global context:
1. `AuthContext`: JWT token, kullanıcı bilgileri, login/logout fonksiyonları
2. `AgencyContext`: Seçili aktif dükkan (agencyId), dükkan listesi, dükkan değiştirme fonksiyonu

TanStack Query (React Query) sunucu durumu (server state) yönetimi için kullanılır: araç listesi,
havuz listesi, dükkan bilgileri vb. Axios interceptor JWT token'ı her isteğe ekler ve 401
yanıtlarda login'e yönlendirir.

**Rationale**: Context API hafif ve yeterli (2 global state). TanStack Query cache invalidation,
background refetch ve optimistic updates sağlar. Redux gibi ağır bir store gereksiz.

**Alternatives Considered**:
- **Redux Toolkit**: Küçük-orta ölçekli uygulama için overkill.
- **Zustand**: Daha minimal ama Context API + TanStack Query ihtiyaçları karşılıyor.
- **SWR**: TanStack Query kadar olgun değil; mutation desteği daha zayıf.

---

## R8: Sigorta/Muayene Compliance Alert Mekanizması

**Decision**: `fleet-service` içinde bir Spring `@Scheduled` cron job günde bir kez çalışır ve tüm
araçların sigorta/muayene tarihlerini kontrol eder. 30 gün içinde dolacak veya süresi geçmiş
araçlar için `fleet.vehicle.compliance-alert` Kafka event'i fırlatılır.
`notification-service` bu event'i dinleyip araç sahibi kullanıcıya (agency owner) uyarı
e-postası gönderir.

Frontend tarafında: API yanıtlarında araçlarla birlikte dönen tarih bilgileri üzerinden
React bileşenleri hesaplama yaparak badge (uyarı rozeti) gösterir — backend'ten ayrı bir
"uyarı" endpointi gerekmez.

**Rationale**: Cron-based batch kontrol basit ve güvenilirdir. Frontend badge'leri API'den dönen
tarih alanlarına dayanır, ayrı bir endpoint yükü oluşturmaz.

**Alternatives Considered**:
- **Event-driven tarih kontrolü (araç eklendiğinde/güncellendiğinde scheduled task oluşturma)**:
  Daha karmaşık; günlük batch check başlangıç için yeterli.
- **Frontend-only uyarı (e-posta olmadan)**: E-posta bildirimi kullanıcının platforma girmeden
  de haberdar olmasını sağlar.

---

## R9: Structured JSON Logging ve Trace ID Propagation

**Decision**: Tüm Spring Boot servislerinde Logback ile `logstash-logback-encoder` kullanılarak
JSON formatında log üretilir. Micrometer Tracing (OpenTelemetry bridge) ile `traceId` ve `spanId`
otomatik olarak MDC'ye eklenir ve her log satırında yer alır. Kafka header'larında `traceparent`
(W3C Trace Context) taşınarak consumer tarafında trace devam ettirilir.

**Rationale**: Constitution Principle V — structured JSON loglama ve trace propagation zorunlu.
`logstash-logback-encoder` Spring Boot ekosisteminde standart çözüm. W3C Trace Context Kafka
entegrasyonu Micrometer ile out-of-the-box desteklenir.

---

## R10: RFC 7807 Problem Details Uygulaması

**Decision**: `common-dto` modülünde Spring Framework'ün built-in `ProblemDetail` sınıfı kullanılır
(Spring 6+ native support). Her serviste `@ControllerAdvice` bazlı global exception handler:
- Validation hataları → 400 + `invalidParams` extension field
- Not found → 404
- Unauthorized / Forbidden → 401 / 403
- Conflict (duplicate email, plate) → 409
- Internal error → 500

`type` alanı URI formatında: `urn:rentacar:problem:<domain>:<error-code>`
Örn: `urn:rentacar:problem:identity:email-already-exists`

**Rationale**: Constitution Principle VI — RFC 7807 zorunlu. Spring 6'nın native ProblemDetail
desteği ek kütüphane gerektirmez. Frontend Axios interceptor'da tutarlı hata ayrıştırma sağlar.

---

## All NEEDS CLARIFICATION: Resolved

Tüm teknik bağlamlar netleştirilmiş, açık kalan soru bulunmamaktadır.

