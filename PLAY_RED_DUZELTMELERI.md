# Play red sebepleri — düzeltme durumu

## 1. Play Billing Library 8.0.0+ — ✅ kodda çözüldü

RevenueCat `8.20.0` transitif olarak `com.android.billingclient:billing:7.1.1` çekiyordu.
RevenueCat `10.19.1`'e yükseltildi → `billing:8.3.0`.

- `gradle/libs.versions.toml`: `revenuecat = "10.19.1"`
- `app/build.gradle.kts`: `versionCode = 9` (8 reddedildi, yeni yükleme için artmalı)

Doğrulama:
```
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep billingclient
# com.android.billingclient:billing:8.3.0
```
Kaynak kodda API kırılması yok — `assembleRelease` + `testReleaseUnitTest` yeşil.

---

## 2. Gizlilik politikası geçersiz — ✅ yayında

Sorun: eski sayfa (Google Sites) uygulamayı ve geliştiriciyi adıyla anmıyordu.

Yeni sayfalar ayrı bir repoda (`bsenturk/varliktakibi-legal`) GitHub Pages ile
yayımlanıyor:

- https://bsenturk.github.io/varliktakibi-legal/privacy.html
- https://bsenturk.github.io/varliktakibi-legal/terms.html

Sayfalar **Varlık Takibi** ve **Burak Ahmet Şentürk** adlarını açıkça içeriyor —
Play'in eşleşme kontrolünü karşılayan kısım bu.

Sayfalar **iOS ve Android'i birlikte** kapsıyor: veri tablosunda platform farkları
(IDFA/ATT ile AAID, identifierForVendor ile Android ID, APNs ile FCM, App Store ile
Play Billing) ayrı ayrı belirtildi. Aynı URL'ler App Store Connect'te de kullanılabilir.

Uygulama içindeki Ayarlar ekranı da bu URL'lere bağlandı
(`SettingsScreen.kt` → `PRIVACY_URL`, `TERMS_URL`). Destek e-postası da politikayla
aynı adrese çekildi (`SUPPORT_EMAIL`).

Kalan adım: Play Console → App content → Privacy policy → yeni URL'i kaydet →
Publishing overview → Send for review.

---

## 3. Data safety formu eksik — ⚠️ Play Console'da doldurulacak

Play'in bulduğu: "Device or other IDs not declared". Kod bunu doğruluyor:

| Nereden | Ne gidiyor |
|---|---|
| `play-services-ads` (AdMob) + `AD_ID` izni (manifest) | Reklam kimliği |
| `PushRegistrar.deviceId()` → `Settings.Secure.ANDROID_ID` + FCM token → Supabase | Cihaz kimliği |
| `firebase-analytics` / `firebase-crashlytics` | Firebase installation ID |

App content → Data safety'de işaretlenecekler:

**Cihaz veya diğer kimlikler → Cihaz veya diğer kimlikler**
- Toplanıyor: Evet · Paylaşılıyor: Evet (AdMob → reklam ortakları)
- Amaç: Reklamcılık veya pazarlama, Analiz, Uygulama işlevselliği
- Zorunlu mu: Kullanıcı isteğe bağlı seçemiyor → "Required"

**Uygulama etkinliği → Uygulama etkileşimleri**
- Toplanıyor: Evet · Paylaşılıyor: Hayır · Amaç: Analiz, Uygulama işlevselliği

**Uygulama bilgileri ve performansı → Çökme günlükleri + Tanılama**
- Toplanıyor: Evet · Paylaşılıyor: Hayır · Amaç: Uygulama işlevselliği

**Finansal bilgiler → Satın alma geçmişi**
- Toplanıyor: Evet · Paylaşılıyor: Hayır · Amaç: Uygulama işlevselliği

**Güvenlik bölümü**
- Veriler aktarımda şifreleniyor: Evet (tümü HTTPS)
- Kullanıcı veri silinmesini isteyebilir: uygulama hesap açmıyor; destek
  e-postası üzerinden silme talebi kabul ediliyorsa Evet.

Sonra: Publishing overview → Send for review.
