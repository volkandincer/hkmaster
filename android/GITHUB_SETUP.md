# GitHub Packages Setup for Masterpass Android SDK

## Token Oluşturma

1. GitHub hesabınıza giriş yapın: https://github.com
2. Sağ üst köşedeki profil resminize tıklayın → "Settings"
3. Sol menüden "Developer settings" → "Personal access tokens" → "Tokens (classic)"
4. "Generate new token" butonuna tıklayın
5. Token ayarları:
   - **Note**: `Android Maven Access` (veya istediğiniz bir isim)
   - **Expiration**: İhtiyacınıza göre seçin
   - **Scopes**: 
     - ✅ `read:packages` (zorunlu)
     - ✅ `repo` (private repository için gerekli)
6. "Generate token" butonuna tıklayın
7. **Token'ı kopyalayın** (bir daha görüntülenemez!)

## Güvenlik: Token Yönetimi

### ⚠️ ÖNEMLİ GÜVENLİK NOTLARI:

1. **Token ASLA Git'e commit edilmemeli**
2. **Token scope'ları minimum olmalı** (sadece `read:packages` ve `repo`)
3. **Token'ı düzenli olarak yenileyin**
4. **Sızdırıldıysa hemen revoke edin**

### Yöntem 1: Environment Variables (ÖNERİLEN - En Güvenli)

Terminal'de environment variable olarak ayarlayın:

```bash
# macOS/Linux
export GITHUB_USERNAME=your_username
export GITHUB_TOKEN=ghp_your_token_here

# Windows (PowerShell)
$env:GITHUB_USERNAME="your_username"
$env:GITHUB_TOKEN="ghp_your_token_here"
```

**Kalıcı yapmak için:**
- **macOS/Linux**: `~/.zshrc` veya `~/.bashrc` dosyasına ekleyin
- **Windows**: System Environment Variables'dan ekleyin

**Avantajları:**
- ✅ Git'e hiçbir şey commit edilmez
- ✅ Her geliştirici kendi token'ını kullanır
- ✅ CI/CD için kolay entegrasyon
- ✅ Token'lar sistem seviyesinde yönetilir

### Yöntem 2: local.properties (Alternatif)

`android/local.properties` dosyasına ekleyin:

```properties
github.username=YOUR_GITHUB_USERNAME
github.token=ghp_YOUR_TOKEN_HERE
```

**Not:** `local.properties` dosyası `.gitignore`'da olduğu için Git'e commit edilmez. Ancak yine de environment variables daha güvenlidir.

## SDK Versiyonu

Şu anki SDK versiyonu: `1.0.0`

Versiyonu güncellemek için `android/app/build.gradle` dosyasındaki dependency'yi değiştirin:

```gradle
implementation("com.masterpass.turkiye:android:1.0.0")
```

## Sorun Giderme

### "Could not resolve" hatası alıyorsanız:
1. Token'ın doğru olduğundan emin olun
2. Token'ın `read:packages` ve `repo` izinlerine sahip olduğunu kontrol edin
3. `local.properties` dosyasının doğru formatta olduğunu kontrol edin
4. Gradle sync yapın: Android Studio → File → Sync Project with Gradle Files

### "401 Unauthorized" hatası:
- Token'ın süresi dolmuş olabilir, yeni token oluşturun
- Token'ın doğru izinlere sahip olduğunu kontrol edin

