#!/usr/bin/env python3
"""A Ctrl+C-equivalent signal cancels one transfer while another keeps running."""
import pathlib
import signal
import subprocess
import sys
import tempfile
import time

with tempfile.TemporaryDirectory(prefix="rclone-stop-") as directory:
    root = pathlib.Path(directory)
    source = root / "source"
    source.mkdir()
    (source / "sample.bin").write_bytes(b"x" * 10_000_000)
    children = []
    try:
        for name in ("selected", "other"):
            children.append(subprocess.Popen(
                [sys.argv[1], "copy", str(source), str(root / name),
                 "--config", str(root / "empty.conf"), "--bwlimit", "1M",
                 "--use-json-log", "--stats", "1s"],
                stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
            ))
        time.sleep(1.5)
        selected, other = children
        assert selected.poll() is None and other.poll() is None
        selected.send_signal(signal.SIGSTOP)
        selected.send_signal(signal.SIGCONT)
        selected.send_signal(signal.SIGINT)
        selected.communicate(timeout=4)
        assert selected.returncode != 0, "Cancellation must not report completion"
        assert other.poll() is None, "Stop must not signal the other transfer"
        other.communicate(timeout=20)
        assert other.returncode == 0
        assert (root / "other" / "sample.bin").read_bytes() == (source / "sample.bin").read_bytes()
        print("PASS: paused child interrupted, other transfer finishes unchanged")
    finally:
        for child in children:
            if child.poll() is None:
                child.kill()
            child.communicate()
