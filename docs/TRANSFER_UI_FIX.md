# Transfer and UI corrections — 2.1.2

The user reported successful Google Drive connection on Android after testing
the setup correction. Device acceptance for the following changes is pending.

- Reproduced the progress bug with a real rclone v1.75.1 throttled local copy:
  `--progress --stats-one-line` produced plain terminal redraws during the copy
  and JSON stats only at the end. Those redraws did not have the `Transferred:`
  prefix required by the fallback parser. The GUI now uses periodic JSON stats
  without terminal progress flags. Managed stats options cannot disable them.
- Screens and notifications use the same global byte/file counters. Individual
  file percentages cannot become the whole job percentage. A growing total may
  reduce the displayed percentage; successful process exit sets completion.
- Speeds use decimal MB/s (1 MB = 1,000,000 bytes), including zero speeds.
- Card and detail percentage labels are inside the right edge of the bar,
  centered vertically, with a heavier font and no font padding. Bar height
  accounts for font scaling. Unknown totals show an em dash rather than a false
  known percentage; notifications show Preparing while totals are unknown.
- Removed the shared 70 dp downward modal offset. Modal surfaces and borders
  use the same 24 dp corner shape, preserving black backgrounds and thin borders.
- Pinned shortcuts get a unique per-card run URI and are refreshed on app launch.
  Cold and warm shortcut intents request immediate execution. Activity recreation
  does not replay the run request. Rapid duplicate starts and queueing an already
  active card are rejected; stop during startup terminates the spawned process.

Validation: unit coverage includes live byte and server-side file progress,
growing totals, completion gating, null ETA, speeds and terminal flag removal.
The real transfer smoke test requires intermediate JSON counters and nonzero
speed while the copy is still running, verifies final counters and file bytes,
and uses only temporary local files. CI runs it alongside the Drive protocol test.

Device checks: update the existing Drive Test app without uninstalling; copy a
new small sample and compare card/detail/notification counters, open the delete
and remote-management dialogs, run a pinned shortcut with the app both closed
and open, rotate during transfer, and check pause/resume/stop. Existing Drive
connections and app data must remain. UI appearance and launcher behavior were
not claimed as tested on an emulator or device here.
