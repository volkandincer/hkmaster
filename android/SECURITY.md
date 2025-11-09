# Güvenlik Rehberi - GitHub Token Yönetimi

## 🔒 Güvenlik Best Practices

### Token Oluştururken:

1. **Minimum Scope Kullanın:**
   - ✅ `read:packages` (paketleri okumak için)
   - ✅ `repo` (private repository için)
   - ❌ `write:packages` (sadece publish için gerekli, okuma için değil)
   - ❌ `admin:repo` (asla gerekmez)

2. **Kısa Süreli Token:**
   - Mümkünse 30-90 gün arası token oluşturun
   - Süresi dolduğunda yeni token oluşturun

3. **Token İsimlendirme:**
   - Açıklayıcı isimler kullanın: `Android-SDK-Access-2025-01`
   - Proje bazında farklı token'lar kullanın

### Token Kullanırken:

1. **ASLA Git'e Commit Etmeyin:**
   - ✅ `local.properties` zaten `.gitignore`'da
   - ✅ Environment variables kullanın
   - ❌ `build.gradle` içine yazmayın
   - ❌ `settings.gradle` içine yazmayın
   - ❌ README'ye yazmayın

2. **Token Sızdırıldıysa:**
   - Hemen GitHub → Settings → Developer settings → Personal access tokens
   - Token'ı revoke edin (iptal edin)
   - Yeni token oluşturun

3. **Token Rotation:**
   - Düzenli olarak (3-6 ayda bir) token'ları yenileyin
   - Eski token'ları revoke edin

## 🛡️ Güvenlik Kontrol Listesi

- [ ] Token sadece `read:packages` ve `repo` scope'larına sahip
- [ ] Token `local.properties` veya environment variable'da (Git'e commit edilmemiş)
- [ ] `.gitignore` dosyasında `local.properties` var
- [ ] Token'ın son kullanma tarihi belirlenmiş
- [ ] Token açıklayıcı bir isme sahip
- [ ] Ekip üyeleri kendi token'larını kullanıyor (paylaşılmıyor)

## 🚨 Acil Durum: Token Sızdırıldı

1. **Hemen revoke edin:**
   ```
   GitHub → Settings → Developer settings → Personal access tokens
   → Token'ı bulun → Revoke
   ```

2. **Yeni token oluşturun** (yukarıdaki adımları takip edin)

3. **Tüm ortamlarda güncelleyin:**
   - Local development
   - CI/CD pipelines
   - Production servers

## 📝 CI/CD için Token Yönetimi

CI/CD sistemlerinde (GitHub Actions, GitLab CI, etc.) token'ı **secrets** olarak saklayın:

**GitHub Actions örneği:**
```yaml
- name: Build Android
  env:
    GITHUB_USERNAME: ${{ secrets.GITHUB_USERNAME }}
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: ./gradlew build
```

**GitLab CI örneği:**
```yaml
variables:
  GITHUB_USERNAME: $GITHUB_USERNAME
  GITHUB_TOKEN: $GITHUB_TOKEN
```

## ✅ Güvenli Kullanım Özeti

- ✅ Environment variables kullanın (en güvenli)
- ✅ Minimum scope ile token oluşturun
- ✅ Token'ları düzenli olarak yenileyin
- ✅ Token'ları asla Git'e commit etmeyin
- ✅ Token sızdırıldıysa hemen revoke edin

