# GitHub Personal Access Token Oluşturma Rehberi

## 📋 Adım Adım Token Oluşturma

### 1️⃣ GitHub'a Giriş Yapın
- https://github.com adresine gidin
- Hesabınıza giriş yapın

### 2️⃣ Ayarlar Sayfasına Gidin
- Sağ üst köşedeki **profil resminize** tıklayın
- Açılan menüden **"Settings"** (Ayarlar) seçeneğine tıklayın

### 3️⃣ Developer Settings'e Gidin
- Sol menüden en alta kaydırın
- **"Developer settings"** seçeneğine tıklayın

### 4️⃣ Personal Access Tokens Bölümüne Gidin
- Sol menüden **"Personal access tokens"** seçeneğine tıklayın
- **"Tokens (classic)"** sekmesine tıklayın
  - (Not: "Fine-grained tokens" değil, "Tokens (classic)" kullanın)

### 5️⃣ Yeni Token Oluşturun
- **"Generate new token"** butonuna tıklayın
- **"Generate new token (classic)"** seçeneğini seçin

### 6️⃣ Token Ayarlarını Yapın

#### Note (Açıklama):
```
Masterpass Android SDK Access
```
veya istediğiniz bir açıklama yazın (örn: `Android SDK 2025`)

#### Expiration (Sona Erme Süresi):
- **30 days** (önerilen - güvenlik için)
- veya **90 days** (daha uzun süre için)
- veya **Custom** (kendi tarihinizi seçin)

#### Scopes (İzinler):
Aşağıdaki kutuları işaretleyin:

✅ **read:packages** 
   - GitHub Packages'ı okumak için (ZORUNLU)

✅ **repo**
   - Private repository'lere erişim için (ZORUNLU - repository private olduğu için)

❌ Diğer izinleri işaretlemeyin (güvenlik için minimum izin)

### 7️⃣ Token'ı Oluşturun
- Sayfanın en altına kaydırın
- **"Generate token"** (yeşil buton) butonuna tıklayın

### 8️⃣ Token'ı Kopyalayın ⚠️ ÖNEMLİ
- Token ekranda görünecek (örn: `ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`)
- **HEMEN KOPYALAYIN** - Token bir daha görüntülenemez!
- Token'ı güvenli bir yere kaydedin

### 9️⃣ Token'ı Kullanın

#### Yöntem 1: Environment Variable (Önerilen)
Terminal'de:
```bash
export GITHUB_USERNAME=your_github_username
export GITHUB_TOKEN=ghp_your_copied_token_here
```

#### Yöntem 2: local.properties
`android/local.properties` dosyasına:
```properties
github.username=your_github_username
github.token=ghp_your_copied_token_here
```

## 🔍 Token'ı Kontrol Etme

Token'ın çalışıp çalışmadığını test edin:
```bash
cd android
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep masterpass
```

## ⚠️ Güvenlik Uyarıları

1. **Token'ı ASLA Git'e commit etmeyin**
2. **Token'ı başkalarıyla paylaşmayın**
3. **Token sızdırıldıysa hemen revoke edin:**
   - GitHub → Settings → Developer settings → Personal access tokens
   - Token'ı bulun → "Revoke" butonuna tıklayın

## 🆘 Sorun Giderme

### "401 Unauthorized" hatası:
- Token'ın doğru kopyalandığından emin olun
- Token'ın `read:packages` ve `repo` izinlerine sahip olduğunu kontrol edin
- Token'ın süresinin dolmadığını kontrol edin

### "Could not resolve" hatası:
- Username ve token'ın doğru olduğundan emin olun
- Environment variable'ları kontrol edin: `echo $GITHUB_USERNAME` ve `echo $GITHUB_TOKEN`
- Gradle sync yapın

## 📸 Görsel Rehber

Eğer adımları görsel olarak görmek isterseniz:
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. "Generate new token (classic)" butonuna tıklayın
3. Yukarıdaki ayarları yapın

