# Sadece telefonla APK derleme

Bilgisayar gerekmez. En rahat yol GitHub Actions'tır.

## 1. GitHub'da repo oluştur

GitHub hesabında yeni ve boş bir repository aç. Örnek ad: `RcloneCards`.

## 2. Projeyi repoya koy

ChatGPT'den indirdiğin `RcloneCards-project.zip` dosyasını bir bulut IDE / GitHub Codespaces üzerinden açıp repository köküne çıkar. Repo kökünde şu dosyaları görmelisin:

```text
.github/
app/
scripts/
build.gradle.kts
gradle.properties
settings.gradle.kts
README.md
```

Dosyaları commit edip `main` dalına gönder.

## 3. GitHub Actions'ı çalıştır

GitHub'da:

```text
Actions
  > Build Android APK
  > Run workflow
```

`main` dalını seçip çalıştır.

Workflow ayrıca `main` dalına her push'ta otomatik başlar.

## 4. APK'yı indir

Build yeşil tamamlanınca ilgili workflow run'ını aç:

```text
Artifacts
  > RcloneCards-arm64-debug
```

ZIP'i indir, içinden `app-debug.apk` dosyasını çıkar ve telefona kur.

## 5. İlk açılış

- Ayarlar > Tüm dosyalara erişim: izin ver
- Ayarlar > Bildirimler: izin ver
- Ayarlar > rclone.conf > İçe aktar

Termux config kopyası:

```bash
cp ~/.config/rclone/rclone.conf /storage/emulated/0/Download/rclone.conf
```

## Build hata verirse

Workflow logundaki ilk kırmızı hatayı kopyalayıp ChatGPT'ye gönder. Genellikle tek commit ile düzeltilebilir.
