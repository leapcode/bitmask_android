#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
L10N_DIR="$(cd "$PROJECT_ROOT/../l10n" && pwd || echo Oops...)"
echo $L10N_DIR
if [ ! -d "$L10N_DIR" ]; then
  echo "L10N_DIR not found: you need to checkout https://0xacab.org/leap/l10n/ next to the bitmask_android repository.";
  exit 1
fi

MODE="${1:-normal}" # normal|custom
case "$MODE" in
  normal|custom) ;;
  *) echo "Usage: $0 [normal|custom]"; exit 1 ;;
esac

command -v jq >/dev/null 2>&1 || { echo "Missing jq"; exit 1; }

# ensure the right l10n branch is checked out
cd $L10N_DIR
if [ ${MODE} == "normal" ]; then
  git checkout bitmask-playstore
  git pull
  SRC_GLOB="$L10N_DIR/bitmask-playstore-metadata/*.json"
else
  git checkout bitmask.riseupvpn-playstore-metadata
  git pull
  SRC_GLOB="$L10N_DIR/bitmask.riseupvpn-playstore-metadata/*.json"
fi
cd -

OUT_BASE="$PROJECT_ROOT/src/${MODE}ProductionFat/fastlane/metadata/android"

# iterate through translations and parse them to fastlane files
for json in $SRC_GLOB; do
  [ -e "$json" ] || break
  lang="$(basename "$json" .json)"
  echo "updating meta data for language $lang"

  if [ ${lang} == "en" ]; then
    echo "moving default english localizations (en) to $OUT_BASE/en-US"
    lang="en-US"
  fi

  out_dir="$OUT_BASE/$lang"
  mkdir -p "$out_dir"

  jq -r '.full_description'  "$json" > "$out_dir/full_description.txt"
  jq -r '.short_description' "$json" > "$out_dir/short_description.txt"
  jq -r '.title'             "$json" > "$out_dir/title.txt"
done
