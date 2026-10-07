# Doğrulama notları

Bu kaynak paketi hazırlanırken aşağıdaki kontroller yapıldı:

- AndroidManifest ve kaynak XML dosyaları XML parser ile doğrulandı.
- `scripts/build-rclone.sh` için `bash -n` geçti.
- GitHub Actions YAML dosyası parse edildi.
- `ShellWords` çok satırlı, ters eğik çizgili ve boşluk içeren UTF-8 yollarla gerçek Kotlin derleyicisinde test edildi.
- Tüm Kotlin kaynakları Kotlin parser/derleyicisine verildi; Android/Compose classpath bu çalışma ortamında bulunmadığı için beklenen `unresolved reference` hataları oluştu, fakat sözdizimi (`expecting`, kapanmamış blok vb.) hatası görülmedi.
- Rclone Android ARM64 derleme yöntemi, rclone'un kendi GitHub build workflow'undaki `GOOS=android`, `GOARCH=arm64`, `CGO_ENABLED=1`, NDK clang ve `-tags android` yöntemiyle aynı mimariye göre yazıldı.

## Henüz burada yapılamayan tek doğrulama

Bu çalışma ortamında Android SDK/Gradle bağımlılıkları ve ağ erişimi tam bir Android APK build'i için hazır değil. Bu yüzden son `assembleDebug` + fiziksel cihaz kurulum testi burada çalıştırılmadı.

Projede `.github/workflows/android.yml` bunun için hazırdır. GitHub Actions ilk build'i gerçek Android/Compose derlemesini ve rclone ARM64 native binary derlemesini yapacaktır. İlk cihaz testinden çıkan gerçek bir hata olursa workflow loguyla düzeltilmelidir.
