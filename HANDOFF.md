# Basic Rclone Flow — developer handoff

Status as of 2026-10-10: community continuation requested. Drive upload is unresolved. The original goal was an independent Android rclone runner, replacing the need to execute these workflows in Termux, with reusable cards and background controls.

## Source references

| Reference | State |
| --- | --- |
| Earlier app source | Build #20, `082e301de1d4186300bca9cfff3bcb988ac42d07`, version 2.1.0 |
| Latest experimental source | `ba05d294ef50be50c6c7dbe39e8f5faec2e903f7`, version 2.1.4 |
| Development branch | `fix/drive-setup-lifecycle` |
| Review | [Draft PR #1](https://github.com/250siparis-code/BasicRcloneFlow/pull/1) |
| Blocking defect | [Drive upload issue #2](https://github.com/250siparis-code/BasicRcloneFlow/issues/2) |

The experimental branch was not merged as a successful upload fix. Obtain it directly when continuing the investigation.

## Observed failures and unknowns

1. Drive uploads can stall after all processed bytes reach 100%, with one or both files still active. An earlier run completed one file; the latest run completed neither before user cancellation. Transfers are parallel, so this is not established as a strictly second-file sequencing defect.
2. The user reported 800 MB processed versus a 14 MB destination file and, in another occurrence, a completed-file count not matching Drive. Those exact discrepancies were not independently reproduced. Destination existence, successful events and processed bytes need separate evidence.
3. Stop previously closed the app. A possible uncaught closed-pipe IOException path was fixed and SIGINT cancellation was added; no device crash stack trace established the exact cause, and a STOP log alone does not prove that the Activity stayed open.
4. Shared Google Drive client_id warning remains in device logs. Configuration/credentials and API limits are investigation leads, not proven explanations for the stall.
5. Progress/borders/notifications/shortcuts received changes but comprehensive device coverage remains pending. Do not describe every affected scenario as verified.
6. Build #20's signing key was not recovered from the configured CI cache. Current local test APK updates share their own signer; unrelated CI/fork builds may not. Same application ID alone does not permit an upgrade.

## Latest reproduction

```sh
rclone copy ~/storage/shared/DCIM/Camera "gdrive:G.A34/CAMDrive"
```

Two APKs: Micro-G v0.3.1.4.240913 (for RVX).apk, 22.023 MiB; Video to MP3 Converter v2.2.7.1 (Pro).apk, 25.512 MiB.

```text
28.9s: 47.535 MiB / 47.535 MiB, 100%; Transferred: 0 / 2
Both files remained listed under Transferring at 100%.
1m37.9s: same total bytes; Transferred: 0 / 2; both still active
STOP Task stopped by user
```

No Copied event or explicit transfer error appeared in that supplied run. In an earlier run Video to MP3 emitted Copied (new), but Micro-G remained active. The latest run ends by cancellation, not by a successful exit or a network error. It cannot establish whether a longer wait would complete, timeout or retry.

## Attempts and their limits

- Native RC configuration continuation, a ViewModel, staged config and foreground OAuth lifetime handling: user confirmed Google Drive connection succeeds on the experimental build. This does not verify uploading.
- Periodic JSON stats, text fallback parsing, MB/s display and separate completed-file events: local regressions pass. Processed bytes may include in-flight/retried work; never equate them with committed Drive files.
- Card metrics and selectable/copyable expandable logs; complete modal borders and direct-run shortcuts: implemented, device scenarios still need review.
- SIGCONT then SIGINT to the selected validated child PID, bounded force fallback and handled pipe IOExceptions: local process/reader tests pass; exact Android crash outcome needs device confirmation.
- Confirmation-wait UI instead of a misleading zero ETA. For bare copy/sync commands targeting Drive, successful completion events are followed by read-only filtered lsjson name/size checks. This is not a separate content-hash check; complex commands retain rclone semantics. The check cannot run while the original upload is still blocked.

No change to rclone's Drive transport was demonstrated to resolve the stall. Avoid repeatedly changing UI text as a substitute for diagnosing it.

## Verification actually performed

Latest experimental build: 36 JVM tests passed; minified release and isolated driveTest APK builds passed. Native Linux rclone smoke tests covered configuration protocol, periodic stats, local copy results, destination listing, stopping a paused child and leaving another copy running. These are not authenticated Android-to-Drive upload tests. User reports after installation remain the authoritative evidence of the unresolved defect.

## Code map on the experimental branch

- `app/src/main/java/dev/galaxy/rclonecards/service/RcloneService.kt`: child processes, signals, readers, queue, notifications, completion and destination checks.
- `engine/RcloneEngine.kt`: command flags/path normalization and quick native commands.
- `engine/TransferProgress.kt`: stats/text parser and successful-event ledger.
- `engine/TransferProcess.kt`, `engine/TransferVerification.kt`: cancellation/readers and read-only destination checks.
- `engine/RcloneConfigWizard.kt`, `ui/DriveSetupViewModel.kt`: rclone setup protocol and lifecycle.
- `ui/RcloneCardsUi.kt`: cards, details, dialogs, log and remote management UI.
- `scripts/build-rclone.sh`: Android v1.75.1 native build, local/Drive backends only.
- `.github/workflows/android.yml`: cloud build/test pipeline and signing-cache handling.

Paths below `engine/` and `ui/` are relative to `app/src/main/java/dev/galaxy/rclonecards/`. More implementation notes are in the experimental branch's `docs/` directory.

## Continue safely

Use sanitized DEBUG logs without HTTP header/body dumps or credentials, compare native Android CLI behavior with GUI behavior under the same options, and check actual Drive names/sizes after success. Record rclone retries/finalization and app lifecycle events. Preserve remotes, files and the AMOLED design. See [CONTRIBUTING.md](CONTRIBUTING.md) for fork/build steps.
