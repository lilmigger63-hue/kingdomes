#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
output=$(mktemp -d)
trap 'rm -rf "$output"' EXIT
mapfile -t sources < <(find src/main/java/com/kingdomsofminecraft/campaign src/main/java/com/kingdomsofminecraft/persistence src/main/java/com/kingdomsofminecraft/geography src/coreTest/java -name '*.java' -print)
javac -encoding UTF-8 -d "$output" "${sources[@]}"
java -cp "$output" com.kingdomsofminecraft.persistence.CampaignStoreTest
