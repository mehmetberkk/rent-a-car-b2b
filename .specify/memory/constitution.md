<!--
Sync Impact Report:
- Version change: Uninitialized -> 1.0.0
- List of modified principles:
  - PRINCIPLE_1: Added "I. Bağımsız Dağıtım ve SPA Mimarisi (Architectural Independence)"
  - PRINCIPLE_2: Added "II. Veri İzolasyonu ve Şema Yönetimi (Data Isolation & Schema Evolution)"
  - PRINCIPLE_3: Added "III. Stateless Güvenlik ve Kimlik Doğrulama (Stateless Security & Context Propagation)"
  - PRINCIPLE_4: Added "IV. Asenkron İletişim ve Mesaj Dayanıklılığı (Asynchronous Event-Driven Resilience)"
  - PRINCIPLE_5: Added "V. Dağıtık İzlenebilirlik ve Yapılandırılmış Loglama (Distributed Observability)"
  - PRINCIPLE_6: Added "VI. Standart Hata Yönetimi (RFC 7807 Problem Details)"
  - PRINCIPLE_7: Added "VII. Gerçekçi Entegrasyon Testleri (Realistic Integration Testing)"
- Added sections:
  - Teknoloji Standartları ve Kısıtlar (Technology Standards & Constraints)
  - Geliştirme İş Akışı ve Kalite Kapıları (Development Workflow & Quality Gates)
  - Governance
- Removed sections: None
- Follow-up TODOs: None
-->

# Rent-A-Car Platform Constitution

## Core Principles

### I. Bağımsız Dağıtım ve SPA Mimarisi (Architectural Independence)
- **Kural**: Backend servisleri birbirinden bağımsız geliştirilebilir, sürümlenebilir ve deploy edilebilir Spring Boot mikroservisleri olarak kurgulanmalıdır (MUST). Servisler derleme veya çalışma zamanında diğer servislerin kodlarına sıkı sıkıya bağımlı olamaz (MUST NOT). Kullanıcı arayüzü tek sayfa uygulaması (SPA) mimarisinde React ve Tailwind CSS ile geliştirilmelidir (MUST).
- **Gerekçe**: Servislerin bağımsız yaşam döngüsüne sahip olması ekiplerin otonom çalışmasını sağlar, deploy risklerini izole eder ve ihtiyaç duyulan servisin bağımsız ölçeklenmesine imkan tanır. React + Tailwind SPA ise performanslı, modüler ve tutarlı bir kullanıcı deneyimi sunar.

### II. Veri İzolasyonu ve Şema Yönetimi (Data Isolation & Schema Evolution)
- **Kural**: "Database-per-service" prensibi zorunludur; mikroservislerin ortak veritabanı veya paylaşılan tablo kullanması kesinlikle yasaktır (FORBIDDEN). Servisler arası veri paylaşımı yalnızca HTTP API'ları veya Kafka event'leri üzerinden gerçekleştirilmelidir (MUST). Tüm veritabanı şema değişiklikleri ve migrasyonlar Flyway kullanılarak kod deposu içinde versiyonlanmalıdır (MUST).
- **Gerekçe**: Doğrudan veritabanı paylaşımı servisler arasında gizli bağımlılıklar (tight coupling) yaratır ve bağımsız deploy edilebilirliği imkansız kılar. Flyway, tüm ortamlarda deterministik, denetlenebilir ve otomatik şema evrimi sağlar.

### III. Stateless Güvenlik ve Kimlik Doğrulama (Stateless Security & Context Propagation)
- **Kural**: Mimari tamamen stateless JWT (JSON Web Token) tabanlı olmalıdır; sunucu katmanında oturum durumu (session state) tutulamaz (MUST NOT). Kullanıcı kimlik ve ilk imza doğrulaması API Gateway üzerinde merkezi olarak gerçekleştirilmelidir (MUST). API Gateway doğrulanmış kullanıcı bağlamını (User ID, roller, yetkiler) downstream mikroservislere standartlaştırılmış HTTP başlıkları (örn. `X-User-Context`, `X-User-Id`, `X-User-Roles`) ile aktarmalıdır (MUST).
- **Gerekçe**: Kimlik doğrulamasının API Gateway'de merkezileştirilmesi mikroservisleri tekrarlayan güvenlik lojiklerinden kurtarır. Stateless yapı sistemin yatayda sorunsuz ve esnek ölçeklenmesini sağlar.

### IV. Asenkron İletişim ve Mesaj Dayanıklılığı (Asynchronous Event-Driven Resilience)
- **Kural**: Servisler arası asenkron iletişimde Apache Kafka kullanılmalıdır (MUST). Kafka topic adlandırmaları kesinlikle domain standardına uymalıdır (Örn: `<domain>.<subdomain>.<event-type>.<version>`) (MUST). Tüm Kafka Consumer bileşenleri idempotent tasarlanmalıdır; aynı mesajın tekrarlı gelmesi mükerrer işleme veya yan etki üretmemelidir (MUST). Tüm consumer konfigürasyonlarında Dead Letter Topic (DLT) ve kontrollü retry (yeniden deneme) mekanizmaları zorunludur (MUST).
- **Gerekçe**: Dağıtık mimarilerde ağ problemleri ve anlık servis kesintileri doğaldır. Idempotency çifte işlemeyi önlerken; DLT ve retry mekanizmaları mesaj kaybını önler ve sistemin kendi kendini toparlayabilmesini (self-healing) sağlar.

### V. Dağıtık İzlenebilirlik ve Yapılandırılmış Loglama (Distributed Observability)
- **Kural**: Tüm HTTP istekleri ve Kafka event trafiği boyunca uçtan uca Trace/Correlation ID (`traceId`, `spanId`) taşınmalı ve servis sınırları arasında kesintisiz aktarılmalıdır (MUST). Tüm servis logları yapılandırılmış (structured JSON) formatta üretilmelidir (MUST). Log kayıtlarında en az zaman damgası, log seviyesi, servis adı, Trace/Correlation ID ve bağlamsal mesaj yer almalıdır.
- **Gerekçe**: Dağıtık mikroservis yapısında kök neden analizi, performans takibi ve hata tespiti merkezi izlenebilirlik olmadan mümkün değildir. Standart Trace ID ve JSON formatı log toplama ve APM sistemlerinin etkin çalışmasını sağlar.

### VI. Standart Hata Yönetimi (RFC 7807 Problem Details)
- **Kural**: Tüm mikroservisler, HTTP hata yanıtlarında RFC 7807 "Problem Details for HTTP APIs" standardına (`application/problem+json`) uygun yanıt dönmelidir (MUST). Servislere özgü rastgele veya özel hata formatları tanımlanamaz (MUST NOT). Hata yanıtı en az `type`, `title`, `status`, `detail` ve `instance` alanlarını, gerektiğinde validasyon detayları için `invalidParams` alanını içermelidir (MUST).
- **Gerekçe**: İstemcilerin (React SPA) ve diğer mikroservislerin hata durumlarını standart, öngörülebilir ve makine tarafından tutarlı biçimde ayrıştırılabilir bir sözleşmeyle ele almasını güvence altına alır.

### VII. Gerçekçi Entegrasyon Testleri (Realistic Integration Testing)
- **Kural**: Kritik iş akışları, veri erişim katmanı ve Kafka olay iletişimleri için Testcontainers tabanlı entegrasyon testleri zorunludur (MUST). Kritik akış testlerinde PostgreSQL ve Kafka yerine in-memory (H2, sahte/mock broker vb.) taklit çözümler kullanılamaz (MUST NOT); testler gerçek konteyner ortamlarında icra edilmelidir.
- **Gerekçe**: İn-memory araçlar PostgreSQL'in dialect/transaction davranışlarını ve Kafka'nın partition, offset ve yeniden deneme dinamiklerini birebir yansıtamaz. Testcontainers prodüksiyona en yakın doğrulama standardını sağlar.

## Teknoloji Standartları ve Kısıtlar

- **Backend**: Java 21+, Spring Boot 3+ mikroservis mimarisi.
- **Frontend**: React (Vite tabanlı), Tailwind CSS SPA.
- **Veritabanı**: Her mikroservis için bağımsız PostgreSQL veritabanı ("Database-per-service"), Flyway ile versiyonlanan şema göçleri.
- **Asenkron Mesajlaşma**: Apache Kafka, domain standartlı topic adlandırması, idempotent consumer'lar, retry ve DLT desteği.
- **Güvenlik**: Spring Cloud Gateway / Reverse Proxy üzerinde doğrulamalı stateless JWT, downstream servislere HTTP header context propagation.
- **İzlenebilirlik**: Dağıtık Trace/Correlation ID (Micrometer Tracing / OpenTelemetry) ve structured JSON loglama.
- **Hata Formatı**: RFC 7807 Problem Details (`application/problem+json`).
- **Test Araçları**: JUnit 5, AssertJ, Mockito, Testcontainers (PostgreSQL & Kafka).

## Geliştirme İş Akışı ve Kalite Kapıları

- **Veritabanı Değişiklikleri**: Tüm şema güncellemeleri Flyway migration dosyaları aracılığıyla yapılmalıdır. Canlı veya geliştirme veritabanlarına doğrudan manuel DDL uygulanması yasaktır.
- **Kod İncelemeleri (Code Review Gates)**:
  - Mikroservisler arası doğrudan veritabanı erişimi içeren PR'lar derhal reddedilir.
  - Trace ID aktarımını bozan veya structured loglama standardına uymayan kodlar onaylanamaz.
  - Retry ve DLT konfigürasyonu bulunmayan veya idempotent olmayan Kafka consumer PR'ları kabul edilmez.
  - RFC 7807 formatına uymayan API hata yanıtları kalite kapısından geçemez.
  - Kritik akışlarda Testcontainers tabanlı entegrasyon testleri eksik olan geliştirmeler onaylanamaz.
- **Sürekli Entegrasyon (CI)**: Tüm birim testler ve Testcontainers entegrasyon testleri CI pipeline üzerinde başarıyla tamamlanmadan hiçbir kod ana dala (main/master) birleştirilemez.

## Governance

- **Üstünlük**: Bu anayasa (Constitution), projedeki tüm mimari kuralların ve kod geliştirme süreçlerinin en üst otoritesidir. Proje içi yerel tercihler bu anayasadaki kurallarla çelişemez.
- **Değişiklik Prosedürü**: Anayasa maddelerinde yapılacak tüm güncellemeler yazılı dokümantasyon, mimari gerekçe, ekip mutabakatı ve geçiş planı gerektirir.
- **Versiyonlama Politikası**: Semantik Versiyonlama (SemVer) kuralları uygulanır:
  - **MAJOR**: Mevcut prensiplerin kaldırılması veya geriye dönük uyumsuz kural değişiklikleri.
  - **MINOR**: Yeni prensip/bölüm eklenmesi veya mevcut rehberliğin kapsamlı genişletilmesi.
  - **PATCH**: Anlamı değiştirmeyen ifade iyileştirmeleri, yazım düzeltmeleri ve biçimlendirme güncellemeleri.
- **Uyum Denetimi**: Tüm tasarım dokümanları, PR'lar ve özellik spesifikasyonları anayasa ilkelerine tam uyum açısından doğrulanır.

**Version**: 1.0.0 | **Ratified**: 2026-11-04 | **Last Amended**: 2026-11-04
