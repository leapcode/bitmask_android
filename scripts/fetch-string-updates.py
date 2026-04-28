#!/usr/bin/env python3
# commit3_update_strings.py

# expects 1 directory higher to have the l10n repository cloned
# https://0xacab.org/leap/l10n
# run: python3 scripts/fetch-string-updates.py
# will pull in changes, warn about missing languages

import os
import re
import shutil
import sys
from xml.etree import ElementTree as ET

L10N_DIR   = "../l10n/bitmask/l10n/"
RES_DIR    = "./app/src/main/res"
SOURCE_XML = "../l10n/bitmask/strings.xml"
THRESHOLD  = 75


def l10n_to_android_qualifier(name: str) -> str:
    """
    Convert l10n filename to Android resource qualifier.

    Rules (applied in order):
      es-AR   → es-rAR     (xx-RR region)
      fa-IR   → fa-rIR
      nb      → nb         (simple, pass through)
      pt-BR   → pt-rBR
      zh_Hans → zh   (fallback to default, simple script)
      zh_Hant → zh-rTW (Traditional, Taiwan)
    """

    # --- special-case Chinese scripts ---
    if name == "zh_Hans":
        return "zh"

    if name == "zh_Hant":
        return "zh-rTW"

    # xx_Script  (zh_Hans, zh_Hant)
    m = re.fullmatch(r'([a-z]{2,3})_([A-Z][a-z]{3})', name)
    if m:
        return f"{m.group(1)}-r{m.group(2)}"

    # xx-RR  (es-AR, pt-BR, fa-IR, gu-IN)
    m = re.fullmatch(r'([a-z]{2,3})-([A-Z]{2,3})', name)
    if m:
        return f"{m.group(1)}-r{m.group(2)}"

    # simple language code
    if re.fullmatch(r'[a-z]{2,3}', name):
        return name

    raise ValueError(f"Cannot convert l10n name to Android qualifier: {name!r}")


def parse_strings(path: str) -> dict[str, str]:
    tree = ET.parse(path)
    return {
        el.get('name'): (el.text or '').strip()
        for el in tree.findall('.//string')
        if el.get('translatable') != 'false'
    }


def translation_pct(source: dict, trans: dict) -> tuple[int, int, int, int, int]:
    total      = len(source)
    translated = sum(1 for k, v in source.items()
                     if k in trans and trans[k] != v and trans[k] != '')
    missing    = sum(1 for k in source if k not in trans)
    same       = sum(1 for k, v in source.items()
                     if k in trans and trans[k] == v)
    pct        = int(translated / total * 100) if total else 0
    return pct, translated, total, missing, same


def normalize_xml_decl(lines: list[str]) -> list[str]:
    if lines and re.match(r"<\?xml", lines[0]):
        lines[0] = '<?xml version="1.0" encoding="UTF-8"?>\n'
    return lines

source_strings = parse_strings(SOURCE_XML)

updated         = []
below_threshold = []
no_folder       = []
bad_name        = []

for filename in sorted(os.listdir(L10N_DIR)):
    if not filename.endswith('.xml'):
        continue
    stem = filename[:-4]
    if stem == 'strings':
        continue

    src_path = os.path.join(L10N_DIR, filename)

    try:
        qualifier = l10n_to_android_qualifier(stem)
    except ValueError as e:
        bad_name.append(f"  BAD NAME: {e}")
        continue

    dst_dir  = os.path.join(RES_DIR, f"values-{qualifier}")
    dst_path = os.path.join(dst_dir, "strings.xml")

    trans_strings = parse_strings(src_path)
    pct, translated, total, missing, same = translation_pct(source_strings, trans_strings)
    info = f"  {pct:3d}%  {stem:<12} → values-{qualifier:<14} ({translated}/{total} translated, {missing} missing, {same} identical)"

    if not os.path.isdir(dst_dir):
        no_folder.append((pct, info + "  ⚠️  NO RES FOLDER"))
        continue

    if pct < THRESHOLD:
        below_threshold.append((pct, info))
        continue

    with open(src_path, 'r', encoding='utf-8') as f:
        lines = normalize_xml_decl(f.readlines())
    with open(dst_path, 'w', encoding='utf-8') as f:
        f.writelines(lines)

    updated.append((pct, info))


def print_section(title, items):
    print(f"\n=== {title} ({len(items)}) ===")
    for _, line in sorted(items, reverse=True):
        print(line)


print_section(f"UPDATED (≥{THRESHOLD}%)", updated)
print_section(f"BELOW THRESHOLD (<{THRESHOLD}%)", below_threshold)

if no_folder:
    print(f"\n{'!' * 60}")
    print(f"  ACTION REQUIRED — {len(no_folder)} language(s) in Weblate have no res folder.")
    print(f"  Review each and either create the folder or explicitly ignore.")
    print(f"{'!' * 60}")
    for _, line in sorted(no_folder, reverse=True):
        print(line)

if bad_name:
    print(f"\n=== BAD FILENAMES ({len(bad_name)}) ===")
    for line in bad_name:
        print(line)

