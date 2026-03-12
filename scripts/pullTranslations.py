#!/usr/bin/env python3
"""
Pull store metadata translations from the l10n git repo and split into
fastlane-compatible files. Creates a MR against bitmask_android.
"""
__author__ = "kwadronaut, cyberta"
__copyright__ = "Copyright 2026, LEAP"
__license__ = "GPL3 or later"
__version__ = "3"

import os
import re
import json
import shutil
import subprocess
import sys
import argparse
import tempfile
import urllib.request
import urllib.error

def get_script_path():
    return os.path.dirname(os.path.realpath(sys.argv[0]))


# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

APP_CONFIG = {
    "main": {
        "l10n_branch": "bitmask-playstore",
        "metadata_dir": "src/normalProductionFat/fastlane/metadata/android",
    },
    "custom": {
        "l10n_branch": "bitmask.riseupvpn-playstore-metadata",
        "metadata_dir": "src/customProductionFat/fastlane/metadata/android",
    },
}

L10N_REPO = "https://0xacab.org/leap/l10n.git"
GITLAB_API = "https://0xacab.org/api/v4"
BITMASK_ANDROID_PROJECT_ID = "leap%2Fbitmask_android"  

# Play Store doesn't support Cuban Spanish, but maybe other store do
SKIP_LOCALES = {"es_CU"}

# Explicit locale mappings: l10n filename stem -> Play Store / fastlane dir name
# Only entries that differ from the default (underscore->dash) need to be here
LOCALE_MAP = {
    "en":      "en-US",
    "zh_Hans": "zh-CN",
    "zh_Hant": "zh-TW",
    "pt_BR":   "pt-BR",
}


# ---------------------------------------------------------------------------
# Locale helpers
# ---------------------------------------------------------------------------

def stem(filename):
    """Return filename without .json extension."""
    return filename.replace(".json", "")


def to_fastlane_locale(locale_stem):
    """Map a l10n locale stem to a Play Store / fastlane directory."""
    if locale_stem in SKIP_LOCALES:
        return None
    if locale_stem in LOCALE_MAP:
        return LOCALE_MAP[locale_stem]
    # default: replace underscore with dash
    return locale_stem.replace("_", "-")


# ---------------------------------------------------------------------------
# Cloning the l10n repo
# ---------------------------------------------------------------------------

def clone_l10n_repo(branch, token=None):
    """
    Shallow-clone the l10n repo at branch into a temp directory.
    Returns the path to the cloned directory.
    """
    tmpdir = tempfile.mkdtemp(prefix="l10n_")
    repo_url = L10N_REPO
    if token:
        # token into URL 
        repo_url = repo_url.replace("https://", f"https://l10n-lab-weblate:{token}@")

    cmd = [
        "git", "clone",
        "--depth", "1",
        "--branch", branch,
        repo_url,
        tmpdir,
    ]
    print(f"Cloning l10n repo (branch: {branch}) ...")
    subprocess.run(cmd, check=True)
    return tmpdir


# ---------------------------------------------------------------------------
# Processing JSON files
# ---------------------------------------------------------------------------

def process_locale_files(l10n_dir, metadata_dir):
    """
    Read each <locale>.json from l10n_dir, map to fastlane locale name,
    write title.txt / full_description.txt / short_description.txt.
    """
    processed = []
    skipped = []

    for filename in sorted(os.listdir(l10n_dir)):
        if not filename.endswith(".json"):
            continue

        locale_stem = stem(filename)
        fastlane_locale = to_fastlane_locale(locale_stem)

        if fastlane_locale is None:
            skipped.append(locale_stem)
            continue

        json_path = os.path.join(l10n_dir, filename)
        with open(json_path, "r", encoding="utf-8") as f:
            try:
                data = json.load(f)
            except json.JSONDecodeError as e:
                print(f"  WARNING: skipping {filename}: {e}")
                continue

        if not data:
            skipped.append(locale_stem)
            continue

        locale_dir = os.path.join(metadata_dir, fastlane_locale)
        os.makedirs(locale_dir, exist_ok=True)

        wrote_any = False
        for key, out_filename in [
            ("title",             "title.txt"),
            ("full_description",  "full_description.txt"),
            ("short_description", "short_description.txt"),
        ]:
            value = data.get(key)
            if value:
                out_path = os.path.join(locale_dir, out_filename)
                with open(out_path, "w", encoding="utf-8") as f:
                    f.write(value)
                wrote_any = True

        if wrote_any:
            processed.append(f"{locale_stem} -> {fastlane_locale}")
        else:
            skipped.append(locale_stem)

    print(f"\nProcessed: {', '.join(processed)}")
    if skipped:
        print(f"Skipped:   {', '.join(skipped)}")


# ---------------------------------------------------------------------------
# GitLab MR
# ---------------------------------------------------------------------------

def create_mr(token, source_branch, target_branch="master", app_type="main"):
    """Open a merge request on bitmask_android via the GitLab API."""
    url = f"{GITLAB_API}/projects/{BITMASK_ANDROID_PROJECT_ID}/merge_requests" payload = json.dumps({
        "source_branch": source_branch,
        "target_branch": target_branch,
        "title": f"chore: update {app_type} Play Store translations",
        "description": "Automated update from the l10n repo (Weblate).",
        "remove_source_branch": True,
    }).encode("utf-8")

    req = urllib.request.Request(
        url,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "PRIVATE-TOKEN": token,
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req) as resp:
            mr = json.loads(resp.read())
            print(f"\nMR created: {mr['web_url']}")
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"\nFailed to create MR: {e.code} {body}", file=sys.stderr)


def commit_and_push(repo_root, source_branch, app_type):
    """Stage changes, commit, push to a new branch."""
    def git(*args):
        subprocess.run(["git", "-C", repo_root] + list(args), check=True)

    git("checkout", "-b", source_branch)
    git("add", APP_CONFIG[app_type]["metadata_dir"])

    result = subprocess.run(
        ["git", "-C", repo_root, "diff", "--cached", "--quiet"],
        capture_output=True,
    )
    if result.returncode == 0:
        print("No changes to commit.")
        return False

    git("commit", "-m", f"chore: update {app_type} Play Store translations from Weblate")
    git("push", "origin", source_branch)
    return True


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Pull Play Store translations from l10n repo and create MR."
    )
    parser.add_argument(
        "app_type",
        choices=["main", "custom"],
        help="'main' for Bitmask, 'custom' for RiseupVPN",
    )
    parser.add_argument(
        "--token",
        default=os.environ.get("L10N_GITLAB_TOKEN"),
        help="GitLab token for l10n-lab-weblate (or set L10N_GITLAB_TOKEN env var)",
    )
    parser.add_argument(
        "--target-branch",
        default="master",
        help="Target branch for the MR (default: master)",
    )
    parser.add_argument(
        "--mr-branch",
        default=None,
        help="Source branch name for the MR (default: auto-generated)",
    )
    args = parser.parse_args()

    config = APP_CONFIG[args.app_type]
    repo_root = os.path.abspath(os.path.join(get_script_path(), ".."))
    metadata_dir = os.path.join(repo_root, config["metadata_dir"])
    mr_branch = args.mr_branch or f"translations/{args.app_type}-playstore-update"

    # 1. Clone l10n repo
    l10n_dir = clone_l10n_repo(config["l10n_branch"], token=args.token)

    try:
        # 2. Process JSON files -> fastlane txt files
        print(f"\nWriting fastlane metadata to: {metadata_dir}")
        process_locale_files(l10n_dir, metadata_dir)

        # 3. Commit & push
        pushed = commit_and_push(repo_root, mr_branch, args.app_type)

        # 4. Open MR
        if pushed:
            if args.token:
                create_mr(args.token, mr_branch, args.target_branch, args.app_type)
            else:
                print("\nNo token provided — skipping MR creation.")
                print(f"Push branch '{mr_branch}' and open MR manually.")
    finally:
        shutil.rmtree(l10n_dir, ignore_errors=True)
