---
name: Bug report
about: Reproducible Android/rclone behavior, with sanitized evidence
title: ''
labels: ''
assignees: ''
---

Read the known issues at the top of README.md first. For the existing upload stall, add evidence to issue #2 instead of opening a duplicate.

## Versions
- App version, source branch/commit and APK origin:
- Android version/device:
- Bundled rclone version:

## Reproduction
- Exact card command (remove private identifiers where needed):
- Steps:
- Expected behavior:
- Actual behavior:

## Evidence
- Did rclone actually exit? With what status?
- Processed bytes, completed-file count and actual destination names/sizes:
- Sanitized log from start through completion/error/cancellation:
- Did Stop leave the app open?

Do not attach rclone.conf, OAuth tokens, client secrets, full-app backups or signing keys. Build success and OAuth success do not demonstrate upload success.
