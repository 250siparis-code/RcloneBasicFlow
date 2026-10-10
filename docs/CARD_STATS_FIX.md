# Card layout and completion counters — 2.1.3

- Both variants display the requested application name, Basic Rclone Flow.
  The isolated test variant keeps the same application ID, signer and private
  data so updating it preserves the user's tested Drive connection.
- Cards extend downward, with equally weighted Speed, ETA and Completed files
  columns. Labels are dim; values and the right-aligned percentage use the normal
  white text color. The progress bar is below the metrics.
- Live log starts collapsed, can be expanded and selected, and has Copy log.
  The detail screen scrolls so larger fonts and expanded logs do not hide actions.
- Raw rclone stats bytes remain labeled Processed. They are not presented as
  confirmed uploaded file sizes: reads for active/failed/retried transfers are
  different from complete destination files.
- A per-run completion ledger accepts successful Copied/Multi-thread Copied or
  server-side Moved events and deduplicates file names. Partial stats and errors
  cannot enter the ledger. Missing size information remains unknown. Confirmed
  file counts and byte sizes are retained in history; old history remains readable.
- The three-column file metric uses confirmed unique completion events when
  available, otherwise rclone's successful transfer count. Active transfers are
  shown separately in details. The changing queue total is not labeled as the
  total number of files in the destination folder.

The user's specific report of 800 MB processed but only a 14 MB file visible
has not been independently reproduced with their Google account. Pending files
or retries are possible explanations, not a proven diagnosis. Request the task's
Copy log output before claiming the upload issue is resolved. No credentials,
saved remotes, user source files or destination files were changed to investigate.

Validation includes negative completion cases (partial byte stats, failed copy,
missing sizes), duplicate events, simultaneous active files, and actual rclone
completion messages. Visual/device acceptance is still required.
