# Contributing and continuing a fork

Contributions and independent forks are welcome under the existing MIT license. Keep the license and rclone notices in distributions. You can propose changes through a pull request without being granted direct write access to this repository.

## Choose the source deliberately

- `main`: earlier 2.1.0 / Build #20 app source and community handoff documentation.
- `fix/drive-setup-lifecycle`: latest experimental 2.1.4 app, with user-confirmed OAuth setup and unresolved Drive uploads. Draft PR #1 contains the changes and their validation limits.
- Read [HANDOFF.md](HANDOFF.md) and [issue #2](https://github.com/250siparis-code/BasicRcloneFlow/issues/2) before investigating uploads.

## Continue the latest code in your own fork

Fork this public repository on GitHub. Replace YOUR_ACCOUNT in these commands with your account:

```sh
git clone https://github.com/YOUR_ACCOUNT/BasicRcloneFlow.git
cd BasicRcloneFlow
git remote add upstream https://github.com/250siparis-code/BasicRcloneFlow.git
git fetch upstream
git switch -c work/drive-upload upstream/fix/drive-setup-lifecycle
git push -u origin work/drive-upload
```

In your fork, enable GitHub Actions if needed. Run **Actions → Build Basic Rclone Flow → Run workflow**, selecting `work/drive-upload`. The workflow has a manual trigger; you do not need to push unverified changes to this project's main branch to build them. To contribute back, open a PR from your fork and clearly state which base branch and source commit were used.

## Build and test

The workflow installs JDK 17, Gradle 8.9, Android SDK 35, build tools 35.0.0 and NDK 27.2.12479018, then compiles rclone v1.75.1 for ARM64. Use the Go version pinned in the chosen branch's workflow. The native build script retains only local and Drive backends.

The experimental branch checks:

```sh
gradle --no-daemon :app:testDebugUnitTest :app:assembleRelease :app:assembleDriveTest
python3 scripts/test-drive-protocol.py /path/to/rclone-v1.75.1
python3 scripts/test-transfer-progress.py /path/to/rclone-v1.75.1
python3 scripts/test-transfer-stop.py /path/to/rclone-v1.75.1
```

Build the Android native library first using `scripts/build-rclone.sh` and the SDK/NDK environment configured by Actions. The Python scripts use a Linux rclone binary and do not exercise authenticated Android Drive uploads. See [PHONE_BUILD.md](PHONE_BUILD.md) for APK download instructions.

## What a useful fix needs

- Provide sanitized reproduction logs, Android/rclone/app versions and the exact command.
- Demonstrate multiple files present in Drive with correct names and sizes, actual successful completion and consistent counters.
- Test cancellation, background execution and retry behavior on Android.
- Retain AMOLED black, thin complete modal borders and the existing card layout.
- Preserve current remotes and user files. Keep data migration compatible.
- Explain the root cause and distinguish tests that passed from device/cloud behaviors still unverified.

Do not commit OAuth tokens, client secrets, `rclone.conf`, full-app backups or signing keys. A fork APK normally has a different signer: use an isolated application ID for tests and never ask users to delete their installation/data as a workaround.
