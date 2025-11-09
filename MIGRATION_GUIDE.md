# Masterpass SDK Migration Guide

**Tarih**: 2025-11-09  
**Versiyon**: 1.0

Bu dokümantasyon, Masterpass SDK entegrasyonunu asıl projenize taşıma işlemi için hazırlanmıştır.

---

## 📦 Taşınacak Dosyalar

### 1. TypeScript Service & Interfaces (Zorunlu)

```
services/
  └── MasterpassService.ts          ✅ Taşınmalı

interfaces/
  ├── MasterpassInitializeParams.interface.ts    ✅ Taşınmalı
  ├── MasterpassAddCardParams.interface.ts       ✅ Taşınmalı
  └── MasterpassResponse.interface.ts           ✅ Taşınmalı
```

### 2. Native Modules (Zorunlu)

#### iOS
```
ios/
  └── [YourProjectName]/
      ├── RCTMasterpassModule.swift     ✅ Taşınmalı
      └── RCTMasterpassModule.m         ✅ Taşınmalı
```

#### Android
```
android/
  └── app/
      └── src/
          └── main/
              └── java/
                  └── com/
                      └── [yourpackage]/
                          └── MasterpassModule.kt    ✅ Taşınmalı
```

### 3. Native Configuration Files (Zorunlu)

#### iOS
- `ios/Podfile` - Masterpass pod dependency eklenecek
- `ios/[YourProjectName]/Info.plist` - ATS (App Transport Security) ayarları

#### Android
- `android/app/build.gradle` - Masterpass SDK dependency
- `android/settings.gradle` - GitHub Packages repository configuration
- `android/app/proguard-rules.pro` - ProGuard rules
- `android/local.properties` - GitHub token (güvenlik için .gitignore'a eklenmeli)

### 4. Test/Demo Files (Opsiyonel - Sadece Referans İçin)

```
screens/
  └── MasterpassTestScreen.tsx       ⚠️ Opsiyonel (test için)

components/
  ├── MasterpassButton.component.tsx      ⚠️ Opsiyonel (test için)
  └── MasterpassResponseDisplay.component.tsx  ⚠️ Opsiyonel (test için)
```

### 5. Dokümantasyon (Opsiyonel - Referans İçin)

```
COMPREHENSIVE_FUNCTION_AUDIT.md      ⚠️ Opsiyonel
SDK_ANALYSIS.md                      ⚠️ Opsiyonel
FUNCTION_COMPARISON.md               ⚠️ Opsiyonel
```

---

## 🔧 Taşıma Adımları

### Adım 1: TypeScript Dosyalarını Taşıma

1. **Service Dosyası**:
   ```bash
   # Asıl projenize kopyalayın
   cp services/MasterpassService.ts [your-project]/services/
   ```

2. **Interface Dosyaları**:
   ```bash
   # Asıl projenize kopyalayın
   cp interfaces/*.interface.ts [your-project]/interfaces/
   ```

### Adım 2: iOS Native Module Taşıma

1. **Swift Dosyası**:
   ```bash
   cp ios/hkmaster/RCTMasterpassModule.swift [your-project]/ios/[YourProjectName]/
   ```

2. **Objective-C Bridge Dosyası**:
   ```bash
   cp ios/hkmaster/RCTMasterpassModule.m [your-project]/ios/[YourProjectName]/
   ```

3. **Podfile Güncelleme**:
   ```ruby
   # Podfile'a ekleyin
   pod 'Masterpass', :path => '../Pods/Masterpass'
   # veya
   pod 'Masterpass', :git => '[masterpass-pod-repo-url]'
   ```

4. **Info.plist ATS Ayarları**:
   ```xml
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

5. **Pod Install**:
   ```bash
   cd ios && pod install
   ```

### Adım 3: Android Native Module Taşıma

1. **Kotlin Dosyası**:
   ```bash
   cp android/app/src/main/java/com/hkmaster/MasterpassModule.kt [your-project]/android/app/src/main/java/com/[yourpackage]/
   ```

2. **build.gradle Güncelleme**:
   ```gradle
   // android/app/build.gradle
   dependencies {
     implementation 'com.masterpass.turkiye:android:1.0.0'
   }
   ```

3. **settings.gradle Güncelleme**:
   ```gradle
   // android/settings.gradle
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
     }
   }
   ```

4. **local.properties Güncelleme**:
   ```properties
   # android/local.properties
   GITHUB_USERNAME=your-github-username
   GITHUB_TOKEN=your-github-token
   ```

5. **ProGuard Rules**:
   ```proguard
   # android/app/proguard-rules.pro
   -keep class com.masterpass.turkiye.** {*;}
   -dontwarn com.masterpass.turkiye.**
   ```

### Adım 4: Package.json Güncelleme

Gerekli React Native bağımlılıkları zaten mevcut olmalı:
- `react-native` (0.82.1+)
- `react-native-safe-area-context` (opsiyonel, test screen için)

### Adım 5: Import ve Kullanım

```typescript
// Asıl projenizde kullanım
import MasterpassService from './services/MasterpassService';

// Initialize
await MasterpassService.initialize({
  merchantId: 123456,
  terminalGroupId: '5575197921009055554235',
  language: 'tr-TR',
  url: 'https://mp-test-sdk.masterpassturkiye.com/',
  verbose: false, // Android only
  merchantSecretKey: undefined, // Android only (optional)
  cipherText: undefined, // iOS only (optional)
});

// Add Card
await MasterpassService.addCard({
  jToken: 'your-jtoken',
  // ... other params
});
```

---

## ⚠️ Dikkat Edilmesi Gerekenler

### 1. Package/Namespace Değişiklikleri

#### Android
- `com.hkmaster` → `com.[yourpackage]` olarak değiştirin
- `MasterpassModule.kt` dosyasındaki package declaration'ı güncelleyin
- `AndroidManifest.xml`'de gerekirse permission'ları kontrol edin

#### iOS
- Module adı `MasterpassModule` olarak kalabilir
- Eğer değiştirmek isterseniz, `RCT_EXTERN_MODULE` ve class adını güncelleyin

### 2. GitHub Token Güvenliği

- `local.properties` dosyasını `.gitignore`'a ekleyin
- Production'da environment variable kullanmayı düşünün
- Token'ı asla commit etmeyin

### 3. SDK Versiyonları

- iOS: Podfile'da belirtilen versiyon
- Android: `build.gradle`'da belirtilen versiyon (1.0.0)
- Versiyonları güncel tutun

### 4. Test vs Production

- Test URL: `https://mp-test-sdk.masterpassturkiye.com/`
- Production URL: Masterpass tarafından sağlanacak production URL
- `initialize` fonksiyonunda URL'i environment'a göre ayarlayın

### 5. Error Handling

- Tüm fonksiyonlar `try-catch` ile kullanılmalı
- Error mesajları kullanıcıya uygun şekilde gösterilmeli
- SDK hataları (401, TokenIsEmpty, vb.) normal durumlar (test için)

---

## ✅ Taşıma Sonrası Kontrol Listesi

- [ ] TypeScript service ve interface'ler taşındı
- [ ] iOS native module taşındı
- [ ] Android native module taşındı
- [ ] iOS Podfile güncellendi ve `pod install` yapıldı
- [ ] Android build.gradle ve settings.gradle güncellendi
- [ ] GitHub token local.properties'e eklendi (ve .gitignore'a eklendi)
- [ ] ProGuard rules eklendi
- [ ] Package/namespace adları güncellendi
- [ ] iOS build başarılı
- [ ] Android build başarılı
- [ ] Initialize fonksiyonu test edildi
- [ ] En az bir fonksiyon (örn: addCard) test edildi

---

## 📝 Önemli Notlar

1. **Test Screen**: Test screen'i production'a taşımayın, sadece development için kullanın
2. **Dokümantasyon**: Referans için tutabilirsiniz ama production build'e dahil etmeyin
3. **SDK Versiyonları**: Production'a geçmeden önce SDK versiyonlarını kontrol edin
4. **Error Handling**: Production'da kullanıcı dostu error mesajları gösterin
5. **Security**: GitHub token ve diğer sensitive bilgileri asla commit etmeyin

---

## 🚀 Hızlı Başlangıç

1. Dosyaları kopyalayın
2. Package/namespace adlarını güncelleyin
3. Dependencies'leri ekleyin (Podfile, build.gradle)
4. Build alın ve test edin
5. Production URL'lerini ayarlayın

---

## 📞 Destek

Sorun yaşarsanız:
1. `COMPREHENSIVE_FUNCTION_AUDIT.md` dosyasını kontrol edin
2. SDK dokümantasyonunu inceleyin
3. Build loglarını kontrol edin

