# Stop and legacy progress (2.1.4)

Stop marks only the selected card as stopped and sends SIGCONT followed by SIGINT to its validated child PID on a worker thread. It waits up to 2.5 seconds before a force fallback. The activity is never finished by this action. Reader IOExceptions caused by closing child pipes are handled, rather than escaping a reader thread and terminating the application. Cancellation-related exceptions preserve STOPPED status. The exact crash reported on the device still needs device confirmation or a stack trace.

Legacy multiline statistics wrapped in JSON messages now update speed, ETA, elapsed time, byte progress, active files, and completed file counts. Percentages in per-file progress are not treated as global progress, and an increased discovered total can lower the global percentage.

The supplied log shows two APK files totaling 381.154 MiB, both still uploading and zero completed files after 11.9 seconds. It contains neither completion nor a transfer error. It does not establish the cause of the earlier 800 MB / 14 MB discrepancy. Obtain the end-of-run log before claiming that issue is resolved.

Validation: JVM regressions for stream closure, cancellation fallback, selected-child isolation and the supplied progress message; a real rclone process test stopping a paused child while a second copy completes; APK builds.
