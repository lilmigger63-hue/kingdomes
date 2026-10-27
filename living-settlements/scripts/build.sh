#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
if ! command -v javac >/dev/null 2>&1; then
  echo 'Install JDK 25 and set JAVA_HOME before building.' >&2
  exit 1
fi
jdk_version="$(javac -version 2>&1)"
if [[ ! "$jdk_version" =~ ^javac\ 25([.+-]|$) ]]; then
  echo "JDK 25 is required; found $jdk_version." >&2
  exit 1
fi
if [[ $# -eq 0 ]]; then
  set -- build
fi
exec bash ./gradlew "$@" --console=plain
