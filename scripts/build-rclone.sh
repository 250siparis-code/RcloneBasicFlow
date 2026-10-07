#!/usr/bin/env bash
set -euo pipefail

RCLONE_VERSION="${RCLONE_VERSION:-v1.75.1}"
NDK_VERSION="${NDK_VERSION:-27.2.12479018}"
ANDROID_API="${ANDROID_API:-21}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/.rclone-src"
OUT="$ROOT/app/src/main/jniLibs/arm64-v8a/librclone.so"

if [[ -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo "ANDROID_SDK_ROOT tanımlı değil" >&2
  exit 1
fi

NDK="$ANDROID_SDK_ROOT/ndk/$NDK_VERSION"
CC="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${ANDROID_API}-clang"

if [[ ! -x "$CC" ]]; then
  echo "Android NDK clang bulunamadı: $CC" >&2
  exit 1
fi

rm -rf "$SRC"
git clone --depth 1 --branch "$RCLONE_VERSION" https://github.com/rclone/rclone.git "$SRC"
mkdir -p "$(dirname "$OUT")"

# RCLONECARDS_MINIMAL_BACKENDS
cat > "$SRC/backend/all/all.go" <<'RCLONE_BACKENDS'
// Package all imports only the backends required by RcloneCards.
package all

import (
    _ "github.com/rclone/rclone/backend/drive"
    _ "github.com/rclone/rclone/backend/local"
)
RCLONE_BACKENDS

pushd "$SRC" >/dev/null
VERSION="$(git describe --tags --always 2>/dev/null || echo "$RCLONE_VERSION")-cards"

env \
  GOOS=android \
  GOARCH=arm64 \
  CGO_ENABLED=1 \
  CC="$CC" \
  CC_FOR_TARGET="$CC" \
  CGO_LDFLAGS="-fuse-ld=lld -s -w" \
  go build \
    -tags android \
    -trimpath \
    -ldflags "-s -w -X github.com/rclone/rclone/fs.Version=$VERSION" \
    -o "$OUT" \
    .

popd >/dev/null
chmod 0755 "$OUT"
echo "Rclone Android binary hazır: $OUT"
file "$OUT" || true
