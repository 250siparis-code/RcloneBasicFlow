# Build the APK using only a phone

A PC is not required. GitHub Actions can build the APK entirely in the cloud.

## 1. Create a GitHub repository

Create a repository such as `RcloneCards`.

## 2. Push the project

From Termux:

```bash
cd ~/RcloneCards
git init
git branch -M main
git add .
git commit -m "Initial RcloneCards Android app"
gh auth login --hostname github.com --git-protocol https --web
gh auth setup-git
git remote add origin https://github.com/YOUR-USER/RcloneCards.git
git push -u origin main
```

## 3. Build automatically

Every push to `main` starts:

```text
Actions > Build Android APK
```

You can also start it manually with **Run workflow**.

## 4. Download the APK

When the run is green, open:

```text
Artifacts > RcloneCards-arm64-minified
```

Extract and install:

```text
app-release.apk
```

## 5. First launch

In the app:

- Grant **All files access**
- Grant **Notifications**
- Connect Google Drive, or import an existing `rclone.conf`

### Import from Termux

Run:

```bash
CFG="$(rclone config file | tail -n 1)"
cp "$CFG" ~/storage/downloads/rclone.conf
```

Then select:

```text
Settings > Rclone > rclone.conf > Import
```

## If a build fails

Open the failed GitHub Actions run and inspect the first failed step. The job log normally contains the exact Gradle, Kotlin, Android SDK or native-build error.
