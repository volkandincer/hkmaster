# Masterpass SDK Taşıma Checklist

## ✅ Taşınacak Dosyalar Listesi

### 1. TypeScript Dosyaları (Zorunlu)

```
✅ services/MasterpassService.ts
✅ interfaces/MasterpassInitializeParams.interface.ts
✅ interfaces/MasterpassAddCardParams.interface.ts
✅ interfaces/MasterpassResponse.interface.ts
```

**Kopyalama Komutu:**
```bash
# Asıl projenize kopyalayın
cp services/MasterpassService.ts [your-project]/services/
cp interfaces/*.interface.ts [your-project]/interfaces/
```

---

### 2. iOS Native Dosyaları (Zorunlu)

```
✅ ios/hkmaster/RCTMasterpassModule.swift
✅ ios/hkmaster/RCTMasterpassModule.m
```

**Kopyalama Komutu:**
```bash
cp ios/hkmaster/RCTMasterpassModule.swift [your-project]/ios/[YourProjectName]/
cp ios/hkmaster/RCTMasterpassModule.m [your-project]/ios/[YourProjectName]/
```

**Podfile Güncellemesi:**
```ruby
# ios/Podfile içine ekleyin
pod 'Masterpass', :git => 'git@github.com:MasterpassTurkiye/Masterpass-ios-sdk.git'

# use_frameworks! zaten olmalı
use_frameworks! :linkage => :static
```

**Pod Install:**
```bash
cd ios && pod install
```

---

### 3. Android Native Dosyaları (Zorunlu)

```
✅ android/app/src/main/java/com/hkmaster/MasterpassModule.kt
✅ android/app/src/main/java/com/hkmaster/MasterpassPackage.kt (eğer varsa)
```

**Kopyalama Komutu:**
```bash
cp android/app/src/main/java/com/hkmaster/MasterpassModule.kt [your-project]/android/app/src/main/java/com/[yourpackage]/
# Package.kt dosyası varsa onu da kopyalayın
```

**Package Adı Değişikliği:**
- `MasterpassModule.kt` dosyasının başındaki `package com.hkmaster` → `package com.[yourpackage]` olarak değiştirin

---

### 4. Android Configuration Dosyaları (Zorunlu)

#### a) build.gradle
```gradle
// android/app/build.gradle - dependencies bloğuna ekleyin
dependencies {
    implementation 'com.masterpass.turkiye:android:1.0.0'
    // ... diğer dependencies
}
```

#### b) settings.gradle
```gradle
// android/settings.gradle - dependencyResolutionManagement bloğuna ekleyin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url 'https://maven.pkg.github.com/MasterpassTurkiye/Masterpass-android-sdk'
            credentials {
                username = project.findProperty("GITHUB_USERNAME") ?: ""
                password = project.findProperty("GITHUB_TOKEN") ?: ""
            }
        }
        maven { url 'https://jitpack.io' }
    }
}
```

#### c) local.properties
```properties
# android/local.properties - ekleyin (ve .gitignore'a ekleyin!)
GITHUB_USERNAME=your-github-username
GITHUB_TOKEN=your-github-token
```

#### d) proguard-rules.pro
```proguard
# android/app/proguard-rules.pro - sonuna ekleyin
-keep class com.masterpass.turkiye.** {*;}
-dontwarn com.masterpass.turkiye.**
```

---

### 5. iOS Configuration Dosyaları (Zorunlu)

#### a) Info.plist - ATS Ayarları
```xml
<!-- ios/[YourProjectName]/Info.plist -->
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSExceptionDomains</key>
    <dict>
        <key>mp-test-sdk.masterpassturkiye.com</key>
        <dict>
            <key>NSExceptionAllowsInsecureHTTPLoads</key>
            <false/>
            <key>NSIncludesSubdomains</key>
            <true/>
        </dict>
    </dict>
</dict>
```

---

## 🔄 Taşıma Sonrası Yapılacaklar

### 1. Package/Namespace Güncellemeleri

#### Android
- [ ] `MasterpassModule.kt` dosyasında `package com.hkmaster` → `package com.[yourpackage]` değiştir
- [ ] `MainApplication.kt` veya `MainApplication.java` dosyasında package import'u kontrol et

#### iOS
- [ ] Module adı genelde değişmez (`MasterpassModule`), ama kontrol et
- [ ] Xcode'da dosyaların doğru target'a eklendiğini kontrol et

### 2. Build ve Test

#### iOS
- [ ] `cd ios && pod install` çalıştır
- [ ] Xcode'da clean build (`Cmd+Shift+K`)
- [ ] Build al (`Cmd+B`)
- [ ] Initialize fonksiyonunu test et

#### Android
- [ ] Android Studio'da Gradle sync yap
- [ ] Clean build (`Build > Clean Project`)
- [ ] Build al (`Build > Rebuild Project`)
- [ ] Initialize fonksiyonunu test et

### 3. Import ve Kullanım Kontrolü

```typescript
// Asıl projenizde test edin
import MasterpassService from './services/MasterpassService';

// Initialize test
try {
  await MasterpassService.initialize({
    merchantId: 123456,
    terminalGroupId: '5575197921009055554235',
    language: 'tr-TR',
    url: 'https://mp-test-sdk.masterpassturkiye.com/',
    verbose: false, // Android only
    merchantSecretKey: undefined, // Android only (optional)
    cipherText: undefined, // iOS only (optional)
  });
  console.log('✅ Initialize başarılı');
} catch (error) {
  console.error('❌ Initialize hatası:', error);
}
```

---

## ⚠️ Önemli Notlar

1. **GitHub Token Güvenliği**
   - `local.properties` dosyasını `.gitignore`'a ekleyin
   - Token'ı asla commit etmeyin
   - Production'da environment variable kullanmayı düşünün

2. **Test vs Production**
   - Test URL: `https://mp-test-sdk.masterpassturkiye.com/`
   - Production URL: Masterpass tarafından sağlanacak
   - Environment'a göre URL'i ayarlayın

3. **SDK Versiyonları**
   - iOS: Podfile'da belirtilen versiyon
   - Android: `build.gradle`'da `1.0.0` (güncel versiyonu kontrol edin)

4. **Test Screen**
   - `MasterpassTestScreen.tsx` dosyasını production'a taşımayın
   - Sadece development için kullanın

---

## 📋 Son Kontrol Listesi

- [ ] Tüm TypeScript dosyaları kopyalandı
- [ ] iOS native dosyaları kopyalandı
- [ ] Android native dosyaları kopyalandı
- [ ] Package/namespace adları güncellendi
- [ ] iOS Podfile güncellendi ve `pod install` yapıldı
- [ ] Android build.gradle güncellendi
- [ ] Android settings.gradle güncellendi
- [ ] GitHub token local.properties'e eklendi (ve .gitignore'a eklendi)
- [ ] ProGuard rules eklendi
- [ ] iOS Info.plist ATS ayarları eklendi
- [ ] iOS build başarılı
- [ ] Android build başarılı
- [ ] Initialize fonksiyonu test edildi
- [ ] En az bir fonksiyon (örn: addCard) test edildi

---

## 🚀 Hızlı Başlangıç Komutları

```bash
# 1. TypeScript dosyalarını kopyala
cp services/MasterpassService.ts [your-project]/services/
cp interfaces/*.interface.ts [your-project]/interfaces/

# 2. iOS dosyalarını kopyala
cp ios/hkmaster/RCTMasterpassModule.swift [your-project]/ios/[YourProjectName]/
cp ios/hkmaster/RCTMasterpassModule.m [your-project]/ios/[YourProjectName]/

# 3. Android dosyalarını kopyala
cp android/app/src/main/java/com/hkmaster/MasterpassModule.kt [your-project]/android/app/src/main/java/com/[yourpackage]/

# 4. iOS pod install
cd [your-project]/ios && pod install

# 5. Android Gradle sync (Android Studio'da yapılır)
```

---

**Hazır! Artık asıl projenize taşıma işlemine başlayabilirsiniz.** 🎉

