# Validation notes

The project is validated through real GitHub Actions Android builds and physical Android installation tests.

Development checks include:

- AndroidManifest and XML resource parsing
- `bash -n` validation for `scripts/build-rclone.sh`
- GitHub Actions workflow parsing
- Android SDK / NDK setup in GitHub Actions
- ARM64 rclone native compilation
- Kotlin / Jetpack Compose compilation
- APK packaging
- Physical device installation
- Real rclone task execution tests
- Iterative fixes from GitHub Actions and device runtime logs

## Current build strategy

CI builds:

- Android app for `arm64-v8a`
- rclone v1.75.1
- Google Drive + local rclone backends only
- Minified release APK
- Android resource shrinking

Runtime behavior should still be re-tested after major changes because storage permissions, OAuth callbacks, foreground services and launcher shortcuts can vary across Android versions and launchers.
