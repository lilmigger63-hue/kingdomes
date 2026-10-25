#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
toolchains=/workspace/toolchains
if [[ ! -x "$toolchains/jdk-25+36/bin/javac" ]]; then
    download=$(mktemp -d)
    trap 'rm -rf "$download"' EXIT
    base='https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25%2B36/OpenJDK25U-jdk_x64_linux_hotspot_25_36.tar.gz'
    curl --fail --location --retry 2 "$base" -o "$download/jdk.tar.gz"
    curl --fail --location --retry 2 "$base.sha256.txt" -o "$download/jdk.sha256"
    python3 - "$download" <<'PY'
import hashlib, sys
from pathlib import Path
p = Path(sys.argv[1])
expected = (p / 'jdk.sha256').read_text().split()[0]
if hashlib.sha256((p / 'jdk.tar.gz').read_bytes()).hexdigest() != expected:
    raise SystemExit('JDK checksum mismatch')
PY
    mkdir -p "$toolchains"
    tar -xzf "$download/jdk.tar.gz" -C "$toolchains"
fi
export JAVA_HOME="$toolchains/jdk-25+36"
export PATH="$JAVA_HOME/bin:$PATH"
python3 scripts/setup-minecolonies.py
python3 scripts/setup-terrain.py
./scripts/test-core.sh
python3 scripts/gradle-cloud.py build
