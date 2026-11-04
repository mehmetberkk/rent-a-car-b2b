# Feature Specification: B2B Rent-A-Car Filo Yönetimi ve Ortak Araç Havuzu Platformu

**Feature Branch**: `001-b2b-fleet-pool-platform`

**Created**: 2026-11-04

**Status**: Draft

**Input**: User description: "B2B Rent a Car Filo Yönetimi ve Ortak Araç Havuzu Platformu"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hesap Kaydı ve E-posta Aktivasyonu (Priority: P1)

Bir rent-a-car dükkanı sahibi, platforma kayıt olmak istemektedir. Kayıt formunu doldurduktan sonra sistemden bir aktivasyon e-postası alır ve bu bağlantıya tıklayarak hesabını aktif hale getirir. Hesap aktif olmadan sisteme giriş yapamaz.

**Why this priority**: Kullanıcı aktivasyonu tüm işlemlerin önkoşuludur. Sisteme erişimin doğrulanmış kullanıcılarla sınırlı tutulması güvenilir bir B2B ortamının temel şartıdır.

**Independent Test**: Kayıt formu, e-posta gönderim ve aktivasyon akışının tamamlanmasının ardından kullanıcı sisteme giriş yapabiliyorsa bu hikaye bağımsız olarak doğrulanabilir.

**Acceptance Scenarios**:

1. **Given** bir kullanıcı kayıt formunu doldurdu, **When** formu gönderir, **Then** sisteme kayıt edilir, e-posta adresi doğrulanmamış (PENDING) statüsünde kalır ve aktivasyon bağlantısı içeren bir e-posta gönderilir.
2. **Given** bir kullanıcı aktivasyon bağlantısını tıkladı, **When** bağlantı geçerli ve süresi dolmamışsa, **Then** hesap ACTIVE statüsüne geçer ve kullanıcı giriş yapabilir hale gelir.
3. **Given** bir kullanıcının hesabı PENDING statüsünde, **When** sisteme giriş yapmaya çalışır, **Then** giriş reddedilir ve aktivasyon e-postası gönderileceği konusunda bilgilendirici bir mesaj gösterilir.
4. **Given** süresi dolmuş bir aktivasyon bağlantısı, **When** kullanıcı bu bağlantıya tıklar, **Then** bağlantının geçersizliği bildirilir ve yeni aktivasyon e-postası talep etmek için seçenek sunulur.
5. **Given** bir kullanıcı halihazırda sistemde kayıtlı bir e-posta adresiyle kayıt olmaya çalışır, **When** formu gönderir, **Then** bu e-posta adresinin zaten kayıtlı olduğu bildirilir, kayıt işlemi gerçekleşmez.

---

### User Story 2 - Çoklu Dükkan Yönetimi (Priority: P1)

Aktif hesabına giriş yapmış bir dükkan sahibi, birden fazla rent-a-car şubesini (acentesini) tek bir hesap üzerinden ekleyebilir, düzenleyebilir ve bunlar arasında geçiş yapabilir. Her şubenin iletişim bilgileri diğer platforma kayıtlı dükkan sahiplerine görünür durumdadır.

**Why this priority**: Çoklu dükkan yönetimi platformun çekirdek B2B değer önerisinin birinci adımını oluşturur; filo ve havuz işlevleri bu yapı üzerine inşa edilecektir.

**Independent Test**: Kullanıcı en az iki dükkan ekleyip bunlar arasında geçiş yapabiliyorsa, her dükkanın detaylarını ayrı ayrı güncelleyebiliyorsa ve dükkan listesini görebiliyorsa bu hikaye bağımsız olarak doğrulanmıştır.

**Acceptance Scenarios**:

1. **Given** aktif bir dükkan sahibi hesabı, **When** yeni bir dükkan ekleme formunu doldurur (isim, lokasyon, telefon, sorumlu kişi, adres), **Then** dükkan kaydedilir ve kullanıcının dükkan listesinde görünür.
2. **Given** birden fazla dükkanı olan bir kullanıcı, **When** arayüzde dükkanlar arasında geçiş yapar, **Then** seçilen dükkanın verileri arayüzde aktif bağlam olarak gösterilir; diğer dükkanların verileri karışmaz.
3. **Given** bir dükkanın iletişim bilgileri mevcut, **When** başka bir kayıtlı dükkan sahibi platformda bu dükkanı görüntüler (ör. havuz ekranında), **Then** o dükkanın telefon numarası, sorumlu kişi adı ve adresi görünür durumdadır.
4. **Given** bir dükkan sahibi dükkanının bilgilerini güncellemek istiyor, **When** ilgili alanları değiştirir ve kaydeder, **Then** güncellemeler anında yansır ve diğer platformdaki kullanıcılara da güncel bilgiler gösterilir.

---

### User Story 3 - Filo Araç Yönetimi ve Operasyonel Takip (Priority: P2)

Aktif bir dükkan sahibi, seçili dükkanının filoya araç ekleyebilir, mevcut araçların bilgilerini (sigorta, muayene tarihleri dahil) güncelleyebilir ve araçları silebilir. Sigorta veya muayene tarihi geçmiş ya da yaklaşan araçları kolayca fark edebilir.

**Why this priority**: Filo yönetimi havuz özelliğinin ön koşuludur; aynı zamanda operasyonel takip özelliği dükkan sahiplerine doğrudan iş değeri sunar.

**Independent Test**: Bir dükkan sahibi araç ekleyip detaylarını görüntüleyebiliyor, sigorta/muayene tarihlerine göre uyarı görüyorsa ve araç silebiliyorsa bu hikaye bağımsız olarak doğrulanmıştır.

**Acceptance Scenarios**:

1. **Given** seçili aktif bir dükkan, **When** kullanıcı yeni bir araç ekler (marka, model, yıl, plaka, yakıt tipi, vites tipi, günlük kira/havuz bedeli, trafik sigortası bitiş tarihi, kasko bitiş tarihi, muayene geçerlilik tarihi), **Then** araç dükkanın filo listesine eklenir ve tüm bilgiler kaydedilir.
2. **Given** filoda bir araç mevcut, **When** sigorta veya muayene tarihi 30 gün içinde doluyorsa, **Then** arayüzde bu araç için görsel bir uyarı/badge gösterilir.
3. **Given** filoda bir araç mevcut, **When** sigorta veya muayene tarihi geçmişse, **Then** bu araç için arayüzde kritik uyarı (EXPIRED) göstergesi görünür.
4. **Given** filoda araçlar mevcut, **When** kullanıcı bir araç siler, **Then** araç filo listesinden kaldırılır; eğer araç havuzdaysa ("Havuzda" statüsünde) ortak havuzdan da anında kaldırılır.
5. **Given** bir araç mevcut, **When** kullanıcı araç bilgilerini (ör. sigorta bitiş tarihi) günceller, **Then** değişiklikler kaydedilir ve filo listesinde güncel bilgiler gösterilir.

---

### User Story 4 - Araç Durum Yönetimi (Priority: P2)

Bir dükkan sahibi, filodaki her bir aracın durumunu "Uygun", "Dolu" veya "Havuzda" olarak tek tıkla değiştirebilir. Araç "Havuzda" statüsüne alındığında diğer kayıtlı dükkan sahiplerinin görebileceği ortak havuza anında dahil edilir.

**Why this priority**: Durum yönetimi havuz kataloğunu doğrudan beslemektedir; havuzun doğru ve güncel kalması B2B güvenilirliği için kritiktir.

**Independent Test**: Kullanıcı bir aracın statüsünü değiştirebiliyor ve "Havuzda" seçildiğinde araç ortak havuzda görünür hale geliyorsa; "Uygun" veya "Dolu" seçildiğinde havuzdan kalkıyorsa bu hikaye bağımsız olarak doğrulanmıştır.

**Acceptance Scenarios**:

1. **Given** filoda "Uygun" statüsünde bir araç, **When** kullanıcı durumu "Havuzda" olarak değiştirir, **Then** araç ortak havuz ekranında diğer kayıtlı ve aktif dükkan sahiplerine görünür hale gelir.
2. **Given** ortak havuzda listelenen bir araç, **When** sahibi aracın statüsünü "Uygun" veya "Dolu" olarak değiştirir, **Then** araç ortak havuzdan anında kaldırılır; diğer dükkan sahipleri artık bu aracı havuzda göremez.
3. **Given** filodaki bir araç, **When** kullanıcı durum değişikliği yapar, **Then** değişiklik anında (gerçek zamanlı ya da en geç sayfa yenilemesinde) filo listesine yansır.

---

### User Story 5 - Ortak Araç Havuzu Görüntüleme ve İletişim (Priority: P3)

Aktif, doğrulanmış bir dükkan sahibi, platformdaki diğer dükkanların havuza açtığı araçları ortak havuz ekranında listeleyebilir, filtreleyebilir ve ilgilendiği aracın bağlı olduğu dükkanın iletişim kartını görüntüleyerek o dükkanla doğrudan iletişime geçebilir.

**Why this priority**: Havuz görüntüleme platformun temel B2B değer önerisi olan araç paylaşım ekosistemini tamamlar; önceki hikayelerin tamamlanmasını gerektirir.

**Independent Test**: Aktif bir kullanıcı başka dükkanların araçlarını listeleyip filtreleyebiliyor ve iletişim bilgilerine ulaşabiliyorsa bu hikaye bağımsız olarak doğrulanmıştır.

**Acceptance Scenarios**:

1. **Given** ortak havuz ekranı açık, **When** aktif ve doğrulanmış bir dükkan sahibi bu ekranı görüntüler, **Then** kendisine ait araçlar hariç, havuzda olan tüm araçlar listelenir.
2. **Given** ortak havuz listesi, **When** kullanıcı araç türü, yakıt tipi, vites veya şehir/lokasyon gibi kriterlere göre filtreler, **Then** yalnızca seçilen kriterlere uyan araçlar listelenir.
3. **Given** bir araç kartı görüntüleniyor, **When** kullanıcı araç kartına veya dükkan bilgisine tıklar, **Then** o aracın bağlı olduğu dükkanın adı, telefon numarası, sorumlu kişi ve adres bilgilerini içeren iletişim kartı görünür.
4. **Given** aktif olmayan (PENDING veya suspended) bir kullanıcı, **When** ortak havuzu görüntülemeye çalışır, **Then** erişim reddedilir.

---

### Edge Cases

- Bir kullanıcı aynı plakayı aynı dükkan içinde iki farklı araca atamaya çalışırsa ne olur?
- Havuzda listelenen bir araç silinirse ortak havuzdan gerçek zamanlı olarak kaldırılabilir mi?
- Aktivasyon e-postası kullanıcıya ulaşmadıysa veya spam klasörüne düştüyse kullanıcı yeni aktivasyon talebi yapabilir mi?
- Bir dükkanın tüm araçları silinirse dükkan kaydı korunur mu?
- Kullanıcı hesabı silinirse o kullanıcıya ait dükkanlar ve araçlar ne olur?

---

## Requirements *(mandatory)*

### Functional Requirements

**Kimlik ve Hesap Yönetimi**

- **FR-001**: Sistem MUST, rent-a-car dükkan sahiplerinin ad, soyad, e-posta ve şifre bilgileriyle kaydolmasına izin vermelidir.
- **FR-002**: Sistem MUST, kayıt sonrasında kullanıcının e-posta adresine benzersiz, süreli bir aktivasyon bağlantısı göndermelidir.
- **FR-003**: Sistem MUST, aktivasyon bağlantısı doğrulanmadan kullanıcının sisteme giriş yapmasını ve herhangi bir işlem gerçekleştirmesini engellemelidir.
- **FR-004**: Sistem MUST, süresi dolmuş aktivasyon bağlantısı üzerinden yeni aktivasyon e-postası talebini desteklemelidir.
- **FR-005**: Sistem MUST, aynı e-posta adresiyle tekrar kayıt girişimini reddetmeli ve kullanıcıyı bilgilendirmelidir.

**Çoklu Dükkan Yönetimi**

- **FR-006**: Kullanıcı MUST, bir hesap altında birden fazla dükkan (isim, lokasyon/şehir, telefon, sorumlu kişi, adres) oluşturabilmelidir.
- **FR-007**: Kullanıcı MUST, dükkanlarının listesini görebilmeli ve aktif bağlamı (seçili dükkanı) değiştirebilmelidir.
- **FR-008**: Sistem MUST, her dükkanın iletişim bilgilerini (telefon, sorumlu kişi, adres) diğer kayıtlı ve aktif kullanıcılara açık tutmalıdır.
- **FR-009**: Kullanıcı MUST, dükkan bilgilerini (isim, iletişim bilgileri, lokasyon) güncelleyebilmelidir.

**Araç Yönetimi (Filo)**

- **FR-010**: Kullanıcı MUST, seçili dükkanına araç ekleyebilmelidir; zorunlu alanlar: marka, model, yıl, plaka, yakıt tipi (benzin/dizel/elektrik/hibrit), vites tipi (manuel/otomatik), günlük kira/havuz bedeli.
- **FR-011**: Sistem MUST, her araç için trafik sigortası bitiş tarihi, kasko bitiş tarihi ve muayene geçerlilik tarihini saklamalıdır.
- **FR-012**: Sistem MUST, sigorta veya muayene tarihi 30 gün içinde dolacak araçları görsel uyarı ile işaretlemelidir.
- **FR-013**: Sistem MUST, sigorta veya muayene tarihi geçmiş araçları kritik uyarı (EXPIRED) ile işaretlemelidir.
- **FR-014**: Kullanıcı MUST, mevcut araç bilgilerini güncelleyebilmeli ve aracı silebilmelidir.
- **FR-015**: Sistem MUST, aynı dükkan içinde aynı plakaya sahip ikinci bir araç eklenmesini reddetmelidir.

**Araç Durum Yönetimi**

- **FR-016**: Kullanıcı MUST, her bir aracının durumunu "Uygun" (Available), "Dolu" (Rented/Busy) veya "Havuzda" (In Pool) olarak değiştirebilmelidir.
- **FR-017**: Sistem MUST, "Havuzda" statüsüne alınan araçları ortak havuz listesine anında (ya da en geç sayfa yenilemesinde) eklemeli; bu statüden çıkan araçları havuzdan anında kaldırmalıdır.

**Ortak Araç Havuzu (B2B Pool)**

- **FR-018**: Sistem MUST, yalnızca aktif (doğrulanmış) dükkan sahiplerine ortak havuzu görüntüleme izni vermelidir.
- **FR-019**: Ortak havuz ekranı MUST, kullanıcının kendi dükkanlarına ait araçlar hariç tüm "Havuzda" araçları listelemeli ve araç başına marka, model, yıl, günlük bedel ve bağlı dükkanın adını göstermelidir.
- **FR-020**: Sistem MUST, ortak havuzda araç türü, yakıt tipi, vites tipi ve lokasyon/şehir gibi kriterlere göre filtrelemeyi desteklemelidir.
- **FR-021**: Havuz ekranındaki her araç kartı MUST, ilgili dükkanın ad, telefon, sorumlu kişi ve adres bilgilerini içeren bir iletişim kartını görüntüleyebilmelidir.

### Key Entities

- **User (Kullanıcı)**: Sisteme kayıtlı dükkan sahibi. Temel özellikler: ad, soyad, e-posta (benzersiz), şifre (hashlı), hesap statüsü (PENDING / ACTIVE), kayıt tarihi.
- **Agency (Dükkan/Şube)**: Kullanıcıya bağlı rent-a-car şubesi. Temel özellikler: isim, şehir/lokasyon, telefon, sorumlu kişi adı, adres; sahibi olan User ile ilişkili.
- **Vehicle (Araç)**: Bir Agency'e bağlı araç. Temel özellikler: marka, model, yıl, plaka (Agency içinde benzersiz), yakıt tipi, vites tipi, günlük bedel, durum (AVAILABLE / RENTED / IN_POOL), trafik sigortası bitiş tarihi, kasko bitiş tarihi, muayene geçerlilik tarihi.
- **ActivationToken (Aktivasyon Jetonu)**: Kullanıcı aktivasyonu için oluşturulan benzersiz, süreli token. Temel özellikler: token değeri, ilişkili User, oluşturulma tarihi, son geçerlilik tarihi, kullanım durumu.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Kayıt formundan aktivasyon e-postasının kullanıcıya ulaşmasına kadar geçen süre 60 saniyeyi geçmemelidir.
- **SC-002**: Aktivasyon bağlantısı üzerinden hesap aktifleştirme işlemi 3 adımdan fazla kullanıcı etkileşimi gerektirmemelidir.
- **SC-003**: Aktif bir kullanıcı, yeni bir araç ekleme işlemini 2 dakikadan kısa sürede tamamlayabilmelidir.
- **SC-004**: Bir aracın durumu değiştirildikten sonra ortak havuzdaki yansıması en geç 5 saniye içinde gerçekleşmelidir (sayfa yenilemesi dahil).
- **SC-005**: Ortak havuz listesi, 100'den fazla araç içerdiği durumda filtreleme sonuçlarını 2 saniyenin altında göstermelidir.
- **SC-006**: Sigorta veya muayene tarihi geçmiş araçlar filo listesinde %100 oranında görsel uyarıyla işaretlenmelidir.
- **SC-007**: Kullanıcıların %90'ı dükkan ekleme, araç ekleme ve durum değişikliği işlemlerini ilk denemede ek yardım almadan tamamlayabilmelidir.

---

## Assumptions

- Platformun mevcut sürümünde yalnızca Agency Owner rolü mevcuttur; dükkan çalışanı veya yönetici alt rolleri kapsam dışıdır.
- Araçların anlık GPS takibi kapsam dışıdır; durum yönetimi manuel olarak dükkan sahibi tarafından yapılır.
- Havuz üzerinden gerçekleşen araç kiralamalarının finansal mutabakatı (ödeme, fatura) platform kapsamı dışındadır; iletişim bilgileri üzerinden tarafların kendi aralarında düzenlemesi beklenmektedir.
- E-posta aktivasyon bağlantısının geçerlilik süresi 24 saattir (industry standardına uygun varsayılan).
- Platform başlangıç sürümünde Türkçe dili desteklenmektedir; çoklu dil desteği kapsam dışıdır.
- Kullanıcıların stabil internet bağlantısına sahip olduğu varsayılmaktadır; tamamen çevrimdışı (offline) kullanım kapsam dışıdır.
- Araçların havuz bedrini (günlük ücret) belirlemek sahibi olan dükkanın sorumluluğundadır; platform fiyat müzakeresi veya otomatik fiyatlandırma sunmaz.
- Mobil uygulama kapsam dışıdır; platform tarayıcı tabanlı (web) erişim için tasarlanmaktadır. Mobil-uyumlu (responsive) tasarım hedeflenmekle birlikte bu öncelikli bir kısıt değildir.

