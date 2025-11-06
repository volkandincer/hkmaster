# GitHub Repository Kurulumu

## 1. GitHub'da Repository Oluşturma

1. GitHub'a giriş yapın: https://github.com
2. Sağ üstteki "+" butonuna tıklayın
3. "New repository" seçin
4. Repository adı: `hkmaster` (veya istediğiniz isim)
5. Public veya Private seçin
6. **ÖNEMLİ:** "Initialize this repository with a README" seçeneğini işaretlemeyin
7. "Create repository" butonuna tıklayın

## 2. Local Repository'yi GitHub'a Bağlama

### HTTPS ile:
```bash
git remote add origin https://github.com/KULLANICI_ADI/hkmaster.git
git branch -M main
git push -u origin main
```

### SSH ile:
```bash
git remote add origin git@github.com:KULLANICI_ADI/hkmaster.git
git branch -M main
git push -u origin main
```

**Not:** `KULLANICI_ADI` kısmını kendi GitHub kullanıcı adınızla değiştirin.

## 3. Mevcut Durum

- ✅ Commit yapıldı: "feat: Masterpass SDK bridge implementation for iOS and Android"
- ✅ 20 dosya değiştirildi
- ⏳ GitHub remote eklenmesi gerekiyor
