#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
L10N_DIR="$(cd "$PROJECT_ROOT/../l10n" && pwd)"

MODE="${1:-normal}" # normal|custom
case "$MODE" in
  normal|custom) ;;
  *) echo "Usage: $0 [normal|custom]"; exit 1 ;;
esac

SRC_GLOB="$L10N_DIR/bitmask-playstore-metadata/*.json"
OUT_BASE="$PROJECT_ROOT/src/${MODE}ProductionFat/fastlane/metadata/android"

command -v jq >/dev/null 2>&1 || { echo "Missing jq"; exit 1; }

for json in $SRC_GLOB; do
  [ -e "$json" ] || break
  lang="$(basename "$json" .json)"
  echo "updating meta data for langauge $lang"
  out_dir="$OUT_BASE/$lang"
  mkdir -p "$out_dir"

  jq -r '.full_description'  "$json" > "$out_dir/full_description.txt"
  jq -r '.short_description' "$json" > "$out_dir/short_description.txt"
  jq -r '.title'             "$json" > "$out_dir/title.txt"
done
