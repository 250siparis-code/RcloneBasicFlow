# Basic Rclone Flow validation checklist

Version: **1.1.0-beta**

## Build

- [ ] GitHub Actions completes successfully.
- [ ] `app-release.apk` is produced and uploaded.
- [ ] App upgrades over the previous test build with the same package ID and signing key.
- [ ] Launcher label is **Basic Rclone Flow**.

## Clean first launch

- [ ] Task list starts empty.
- [ ] No GA34 / Camera demo cards are restored.
- [ ] Adding a card creates a blank user-owned task.

## Google Drive setup

- [ ] Settings states that Google Drive is the only cloud provider.
- [ ] Google Drive Setup starts the local RC service on `127.0.0.1:5572`.
- [ ] Questions come from rclone `config/create` / `config/update`.
- [ ] Full Access / scope selection is shown when rclone asks it.
- [ ] Service Account JSON can be selected when `service_account_file` is requested.
- [ ] OAuth opens in the browser.
- [ ] OAuth success page offers **Open Basic Rclone Flow**.
- [ ] Deep link returns to the existing app task.
- [ ] Setup is not reported as verified until `rclone lsd remote:` succeeds.
- [ ] A failed live test is shown as failed, not as a successful connection.

## Remote management

- [ ] Manage Google Drive lists configured Drive remotes.
- [ ] Test reports live connectivity.
- [ ] Edit changes only the selected remote block.
- [ ] Delete removes only the selected remote.

## Task execution

- [ ] Run starts a real rclone process.
- [ ] Live progress stays inside the fixed-height card.
- [ ] Pause / Resume / Stop work.
- [ ] Completed cards keep a Run button and can be started again.
- [ ] Missing remote errors point to Google Drive Setup.
- [ ] Queue and scheduling still work.

## Data safety

- [ ] Import/export `rclone.conf` works.
- [ ] Card backup/restore works with an empty or non-empty list.
- [ ] Full-app backup/restore works.
- [ ] Clear All Cards does not touch Drive or local files.
- [ ] Clear App Data does not delete Drive or local files.
