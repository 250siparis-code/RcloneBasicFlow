# Rclone Cards

Rclone Cards, AMOLED siyah arka planlı ve büyük görev kartları üzerinden **gerçek rclone komutları** çalıştıran Android uygulamasıdır.

Bu proje HTML prototipi değildir. GitHub Actions derlemesi sırasında resmi **rclone v1.75.1** kaynak kodu Android ARM64 için derlenir, APK'nın native library dizinine `librclone.so` adıyla paketlenir ve uygulama bu ELF dosyasını `ProcessBuilder` ile doğrudan çalıştırır.

## Ana özellikler

- Saf `#000000` AMOLED ana ekran
- Büyük, isimlendirilebilir görev kartları
- Karta dokununca gerçek `rclone` işlemi başlatma
- Kartı uzun basınca işlem menüsü
- Kart ekle / düzenle / kopyala / yeniden adlandır / renk / simge / sil
- Kart başına gelişmiş `--transfers`, `--checkers`, `--bwlimit` ayarları
- Uygulama genelinde varsayılan `--transfers=4`, `--checkers=8` ayarları
- Serbest rclone komutu: `sync`, `copy`, `move`, `check`, `size`, `delete`, `purge`, `lsjson` vb.
- Tırnaklı yollar ve `\\` + satır sonu ile çok satırlı Termux komutlarını ayrıştırma
- Gerçek arka plan çalışması: Android foreground `dataSync` servisi
- Aktarım sırasında CPU'nun uyumasını engelleyen `PARTIAL_WAKE_LOCK`
- Gerçek rclone JSON stats: yüzde, aktarılmış/toplam byte, hız, ETA, dosya sayısı
- İşlem bitince isteğe bağlı sonuç bildirimi
- Canlı rclone logları ve hata gösterimi
- Durdur / `SIGSTOP` ile duraklat / `SIGCONT` ile devam et
- İş kuyruğu
- Günlük zamanlayıcı ve yeniden başlatma sonrası alarmı tekrar kurma
- Ana ekran kısayolu oluşturma
- `rclone.conf` içe aktar / dışa aktar / uygulama içinde düzenle
- Remote listesini gerçek `rclone listremotes --json` ile gösterme
- Kartları JSON olarak yedekleme / geri yükleme
- Android 11+ için “Tüm dosyalara erişim” ayarına yönlendirme

## Gerçek ilerleme nasıl geliyor?

Kullanıcının komutundaki `--progress` kaldırılır ve uygulama otomatik olarak aşağıdakileri ekler:

```text
--config <uygulamanın private rclone.conf yolu>
--cache-dir <uygulama cache yolu>
--use-json-log
--stats 1s
--stats-log-level NOTICE
--log-level INFO
```

Rclone'un NDJSON log satırlarındaki `stats` nesnesi okunur. Arayüzde görülen yüzde, hız, ETA ve dosya sayısı sahte değer değildir.

## Telefonda, bilgisayarsız APK derleme

Projede hazır `.github/workflows/android.yml` vardır. Workflow:

1. JDK 17 ve Go 1.27.x kurar.
2. Android SDK 35 + NDK 27.2 kurar.
3. Resmi `rclone v1.75.1` kaynağını ARM64 Android için derler.
4. `librclone.so` dosyasını APK içine koyar.
5. Android uygulamasını derler.
6. `RcloneCards-arm64-debug` isimli indirilebilir APK artifact'ı üretir.

Ayrıntılı telefon adımları için `PHONE_BUILD.md` dosyasına bak.

## İlk kurulum

Uygulamayı açtıktan sonra `Ayarlar` bölümünde:

1. **Tüm dosyalara erişim** iznini ver.
2. **Bildirim** iznini ver.
3. Mevcut `rclone.conf` dosyanı içe aktar.

Termux'taki config'i erişilebilir bir yere kopyalamak için:

```bash
cp ~/.config/rclone/rclone.conf /storage/emulated/0/Download/rclone.conf
```

Sonra uygulamada `Ayarlar > rclone.conf > İçe aktar`.

## Kart örneği

```bash
rclone copy \
  "/storage/emulated/0/DCIM/Camera/" \
  "gdrive:G.A34/Dahili Hafıza/DCIM/Camera/" \
  --progress
```

Kart komutu bir **rclone komutudur**, genel amaçlı shell script değildir. `|`, `&&`, `rm` gibi shell işlemleri çalıştırılmaz; böylece karttan yalnızca paketlenmiş rclone yürütülür.

## Android depolama notu

`MANAGE_EXTERNAL_STORAGE`, `/storage/emulated/0/...` ve uygun ikincil depolama yollarına geniş erişim sağlar. Android sistem kısıtları nedeniyle `/Android/data` ve `/Android/obb` gibi bazı alanlar yine kapalı kalabilir. Varsayılan tam-depolama kartı bu nedenle `/Android/**` yolunu dışlar.

## Neden targetSdk 34?

Bu proje kişisel/sideload kullanımına göre ayarlanmıştır: `compileSdk=35`, `targetSdk=34`. Android 15'i hedefleyen (`targetSdk 35+`) `dataSync` foreground servislerinde 24 saatte toplam 6 saatlik arka plan süresi sınırı vardır. Büyük rclone yedeklerinin saatlerce sürebilmesi nedeniyle bu kişisel yapı target 34 kullanır. Play Store'a yayımlanacak sürümde güncel hedef-SDK şartlarına göre mimariyi `user-initiated data transfer` gibi güncel API'lerle yeniden ele almak gerekir.

## Mimari

- Kotlin + Jetpack Compose
- `ForegroundService` (`dataSync`)
- Resmi rclone CLI ARM64 build
- `ProcessBuilder` ile doğrudan argv çalıştırma
- `--use-json-log --stats 1s` ile gerçek ilerleme
- SharedPreferences + JSON kart kalıcılığı
- Uygulama-private `files/rclone/rclone.conf`
- AlarmManager ile günlük görev zamanlama

## Mimari kapsam

Bu sürüm `arm64-v8a` içindir. Galaxy A34/A72 gibi 64-bit ARM Android cihazlar hedeflenmiştir. Diğer mimariler gerektiğinde CI matrix'e kolayca eklenebilir.

## Lisans

Rclone MIT lisanslıdır. Rclone kaynak kodu değiştirilmeden derlenir. `NOTICE.md` dosyasında rclone atfı bulunur.
