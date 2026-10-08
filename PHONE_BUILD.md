# Build Basic Rclone Flow using only a phone

A PC is not required. GitHub Actions builds the Android APK in the cloud.

## Push changes

```bash
cd ~/RcloneCards
git add .
git commit -m "Update Basic Rclone Flow"
git push
```

Every push to `main` starts:

```text
Actions → Build Basic Rclone Flow
```

## Download

When the workflow is green, download:

```text
Basic-Rclone-Flow-v1.1.0-beta-arm64
```

Extract the ZIP and install `app-release.apk`.

## First launch

1. Open **Settings**.
2. Grant **All files access** if your cards use local shared-storage paths.
3. Grant **Notifications** for foreground transfer status.
4. Open **Google Drive Setup**.
5. Complete the questions returned by rclone.
6. Approve Google OAuth in the browser.
7. Return to the app; Basic Rclone Flow performs a live Drive check before reporting the remote as verified.

The current build intentionally supports Google Drive as the only cloud provider.

## Existing Termux rclone config

```bash
CFG="$(rclone config file | tail -n 1)"
cp "$CFG" ~/storage/downloads/rclone.conf
```

Then use **Settings → Rclone → Import rclone.conf**.

Never commit `rclone.conf` or a full-app backup to GitHub. Both can contain OAuth credentials.
