# Drive setup correction — 2.1.1

Base: `082e301de1d4186300bca9cfff3bcb988ac42d07` (Build 20).

## Findings and changes

- The original v1.75.1 RC request returns `client_id` correctly in an isolated
  reproduction. The reported immediate disappearance was not reproduced on an
  Android device here; do not claim its exact cause has been proven.
- Setup previously lived in Compose `remember` and a composition-owned coroutine
  scope. Activity recreation discarded the state. It now belongs to the Activity
  ViewModel; screen routing is saveable. The ViewModel stores no Activity reference.
- A foreground service protects the running OAuth operation while the browser is
  foregrounded. After process death, the UI explains that setup was interrupted;
  it does not pretend a lost operation completed or silently erase a saved remote.
- Successful setup stays visible until Done. The black modal and thin border are
  preserved; the fixed downward offset was removed from the setup modal.
- Start must return a question. Continuation uses `config/update` with the exact
  rclone state/result, including empty optional answers. Invalid/missing states
  and final error responses cannot become successful completion.
- Setup uses a temporary config. Only a finished new remote is appended atomically
  to the main config. Existing remote names are rejected. Closing removes only the
  temporary setup file, never saved remotes or user files.
- Service-account imports get unique filenames so selecting a new account cannot
  overwrite a file used by an existing remote. Encrypted main configs are rejected
  rather than appending plaintext to and corrupting them.
- The loopback RC server uses an available port and a random password instead of
  exposing an unauthenticated fixed port. Browser-launch errors become visible
  setup errors. Client Secret and token inputs are masked.
- Real `rclone lsd remote:` verification is still required for the verified label;
  saved-but-unverified and verified states remain separate.
- CI now includes protocol/unit tests and builds pull requests. Version is 2.1.1,
  versionCode 5. The existing CI signing-key cache is preserved.

## Validation boundaries

The real rclone protocol smoke test uses isolated files and fake credentials. It
checks question order, optional answers, authenticated RC access, the local OAuth
URL, the Google authorization redirect parameters, and cancellation. It does not
approve a real Google account, exchange a real token, or list a real Drive.

Android device acceptance remains required:

1. Install the isolated `Drive Test` APK alongside the existing app. It has a
   separate application ID, private data directory and OAuth return scheme.
   Do not uninstall or clear the existing app. A normal APK can be used as an
   upgrade only if its signing certificate matches the installed version.
2. Use a new remote name; confirm Start Setup shows Client ID, then Client Secret.
3. Rotate on the Client ID screen and while browser approval is pending.
4. Complete Google consent, return to the app, and require the live listing result.
5. Cancel/deny consent and retry; existing remotes/cards must remain unchanged.
6. Reopen the app after process death; it must show interruption, not false success.
7. Test one existing transfer, pause/resume, queue, and schedule on device.

GitHub write attempt returned 403 `Resource not accessible by integration`; no
remote branch, commit or PR was created. The user's uncommitted Termux edits were
not available and were not altered. Do not overwrite them when applying this patch.

## Local build evidence

- `:app:testDebugUnitTest`: 13 tests passed (8 response/protocol, 5 config safety).
- `:app:assembleRelease`: minified release APK built successfully with Gradle 8.9,
  Temurin JDK 17 and Android SDK 35.
- Native `librclone.so` was reused unchanged from the successful Build 20 artifact;
  its artifact SHA-256 was checked before extraction. Native source was not changed.
- The local APK uses a local debug signing key, not the existing CI key. It is not
  offered as an in-place upgrade. Use the existing CI signing cache for delivery.
- Real Google OAuth on Android and regression tests on the user's device remain
  outstanding. Build success and fake-credential redirects do not prove those.

## GitHub validation and signing discovery

- GitHub installation access was restored and PR #1 was created.
- Build #22 passed the unit tests, real rclone protocol test and native/minified
  Android build. Its source tree exactly matched the locally validated tree.
- Build #22 certificate SHA-256 is
  `a7ae858918018155f0f2f0e23f8374310bb5ed9bc817992fe5fa3c427e00fc20`.
  Build #20 used
  `4cacc83550d680f7deb762f85025703433cda75f23ef1047deb841b4bfcc03d5`.
  Therefore Build #22 is not an in-place upgrade for Build #20.
- Both CI logs show a signing-key cache miss and a post-build warning that the
  cached key path did not exist. Preserving the cache step alone did not preserve
  the actual signing key. CI now explicitly creates and uses that cached path.
  This fixes future cache use; it cannot recover the missing Build #20 private key.
- The separate minified `driveTest` variant is provided for device validation
  without touching the existing installation. Its callback scheme is isolated
  in the manifest, Activity, notification and rclone OAuth template.
