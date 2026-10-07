# Rclone Cards

Rclone Cards is a lightweight AMOLED-first Android front end for **real rclone commands**. It is designed around editable task cards instead of a full file manager: tap a card to run a predefined sync, copy, move, check, size, delete, purge or other rclone operation, then watch live transfer progress directly on the card.

Created by **BlackWare** with **OpenAI ChatGPT**.

## Highlights

- Pure `#000000` AMOLED interface
- Large editable task cards
- Real rclone execution, not a web mockup
- Live percentage, transferred bytes, speed, ETA, file count and logs
- Android foreground `dataSync` service for long-running jobs
- Pause, resume, stop and queue support
- Daily scheduling
- Per-card home-screen shortcuts
- `rclone.conf` import, export and in-app editing
- Google Drive connection flow inside the app
- JSON card backup and restore
- Per-card `--transfers`, `--checkers` and `--bwlimit`
- Termux-style quoted paths and multiline rclone commands
- Automatic conversion of `~/storage/shared/...` paths to `/storage/emulated/0/...`
- ARM64 Android build with a bundled rclone engine
- Minified release APK with unused Android resources removed
- rclone build limited to the Google Drive and local backends to keep the APK smaller

## How it works

During GitHub Actions builds, the official **rclone v1.75.1** source is compiled for Android ARM64 and packaged as `librclone.so`. The Android app executes that binary directly with `ProcessBuilder`.

User-entered `--progress` is removed because the app reads structured rclone JSON statistics instead. The following options are added automatically when needed:

```text
--config <app-private rclone.conf>
--cache-dir <app-private cache>
--use-json-log
--stats 1s
--stats-log-level NOTICE
--log-level INFO
```

The UI reads rclone's NDJSON `stats` data to display real progress instead of simulated values.

## First setup

Open **Settings** in the app and:

1. Grant **All files access** if you use local storage paths.
2. Grant **Notifications** so long-running foreground tasks can report status.
3. Connect Google Drive inside the app, or import an existing `rclone.conf`.

### Import an existing Termux config

The same instruction is available inside the app at:

```text
Settings > Rclone > Import from Termux
```

Run:

```bash
CFG="$(rclone config file | tail -n 1)"
cp "$CFG" ~/storage/downloads/rclone.conf
```

Then use:

```text
Settings > Rclone > rclone.conf > Import
```

## Example task

```bash
rclone copy \
  "/storage/emulated/0/DCIM/Camera/" \
  "gdrive:G.A34/Dahili Hafıza/DCIM/Camera/" \
  --progress
```

Task commands are **rclone commands**, not arbitrary shell scripts.

## Build on a phone

Every push to `main` starts `.github/workflows/android.yml`.

The workflow:

1. Sets up JDK 17 and Go.
2. Installs Android SDK 35 and NDK 27.2.
3. Builds rclone v1.75.1 for Android ARM64.
4. Builds a minified Android release APK.
5. Uploads the `RcloneCards-arm64-minified` artifact.

See [PHONE_BUILD.md](PHONE_BUILD.md).

## Android storage

`MANAGE_EXTERNAL_STORAGE` gives broad access to shared storage paths, but Android can still restrict areas such as `/Android/data` and `/Android/obb`.

## Architecture

- Kotlin
- Jetpack Compose
- Foreground `dataSync` service
- Native ARM64 rclone CLI
- `ProcessBuilder`
- JSON stats parsing
- SharedPreferences + JSON persistence
- App-private `rclone.conf`
- AlarmManager scheduling
- Pinned home-screen shortcuts
- R8 minification and resource shrinking

## Supported ABI

The current build targets **arm64-v8a**.

## License

Rclone is licensed under the MIT License. See `NOTICE.md` and `RCLONE_LICENSE.txt`.
