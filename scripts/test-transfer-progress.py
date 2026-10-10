#!/usr/bin/env python3
"""Verify periodic global JSON stats arrive before a real copy exits."""
import json
import pathlib
import subprocess
import sys
import tempfile

with tempfile.TemporaryDirectory(prefix="rclone-progress-") as directory:
    root = pathlib.Path(directory)
    source = root / "source"
    source.mkdir()
    (source / "sample.bin").write_bytes(b"x" * 6_000_000)
    process = subprocess.Popen(
        [sys.argv[1], "copy", str(source), str(root / "destination"),
         "--config", str(root / "empty.conf"), "--use-json-log",
         "--stats", "1s", "--stats-log-level", "NOTICE",
         "--log-level", "INFO", "--bwlimit", "1M"],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
    )
    records = []
    live = False
    for line in process.stderr:
        record = json.loads(line)
        stats = record.get("stats")
        if stats:
            records.append(stats)
            if 0 < stats["bytes"] < stats["totalBytes"] and stats["speed"] > 0:
                live = live or process.poll() is None
    assert process.wait(timeout=20) == 0
    assert process.stdout.read() == ""
    assert live, "No intermediate byte/speed stats arrived while copy was running"
    assert len(records) >= 2
    assert records[-1]["bytes"] == 6_000_000
    assert records[-1]["transfers"] == 1
    assert (root / "destination" / "sample.bin").read_bytes() == (source / "sample.bin").read_bytes()
    print("PASS: periodic live JSON stats, bytes, total, speed and successful copy")
