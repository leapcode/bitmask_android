#!/usr/bin/env python3
""
__author__ = "kwadronaut, cyberta"
__copyright__ = "Copyright 2023, LEAP"
__license__ = "GPL3 or later3 or later3 or later"
__version__ = "2"

import os
import re
import argparse
import json
import subprocess
import sys

def get_script_path():
    return os.path.dirname(os.path.realpath(sys.argv[0]))


# Set the path to the res directory containing different language folders
main_res_dir = get_script_path() + "/../app/src/main/res"
custom_res_dir = get_script_path() + "/../app/src/custom/res"

# List all valid locale folders in the res directory
# We don't want to create a translated store listing without localized app
def list_locales(app_type):
    locales = []
    if app_type == "main":
        res_dir = main_res_dir
    elif app_type == "custom":
        res_dir = custom_res_dir
    else:
        raise ValueError("Invalid app type. Use 'main' or 'custom'.")

    valid_locale_pattern = re.compile(r'^values-(?P<language>[a-z]{2})(-(?P<script>[a-zA-Z]{4}))?(-r(?P<region>[a-zA-Z]{2}))?$')
    for folder in os.listdir(res_dir):
        if valid_locale_pattern.match(folder):
            locale_code = valid_locale_pattern.match(folder).group(0)
            locales.append(locale_code)

    # add default locale
    locales.append("en-US")

    return locales

# Create empty JSON file for each locale metadata directory
# If there's no file, tx will skip the translations
def create_metadata_files(locales, app_type):
    if app_type == "main":
        metadata_dir = get_script_path() + "/../src/normal/fastlane/metadata/android"
    elif app_type == "custom":
        metadata_dir = get_script_path() + "/../src/custom/fastlane/metadata/android"
    else:
        raise ValueError("Invalid app type. Use 'main' or 'custom'.")
    for locale_code in locales:
        locale_dir_name = map_android_locale_dir_to_metadata_dir(locale_code)
        file_path = os.path.join(metadata_dir, locale_dir_name, f"store-meta-{locale_dir_name}.json")
        if not os.path.exists(file_path):  # Check if the file already exists
            os.makedirs(os.path.dirname(file_path), exist_ok=True)
            with open(file_path, "w", encoding="utf-8") as file:
                file.write("{}")  # Write an empty JSON object to the file

def map_android_locale_dir_to_metadata_dir(locale_code):
        locale_region_pattern = re.compile(r'^values-(?P<language>[a-z]{2})(-r(?P<region>[a-zA-Z]{2}))$')
        pattern_match = locale_region_pattern.match(locale_code)
        if pattern_match:
            # locale directory contains a region, replace values-<language>-r<region> with <language>-<region>
            locale_dir_name = locale_code.replace("values-" + pattern_match.group("language") + "-r", pattern_match.group("language") + "-")
            # print(f"locale with region suffix: '{locale_code}' - new metadata locale dir name: {locale_dir_name}")
        else:
            # Remove "values-" prefix from the locale directory name
            locale_dir_name = locale_code.replace("values-", "")
            # print(f"new metadata locale dir name: '{locale_dir_name}'")
        return locale_dir_name


# Split JSON data and save to separate files for each locale
def split_json_and_save(locales, metadata_dir):
    for locale_code in locales:
        locale_dir_name = map_android_locale_dir_to_metadata_dir(locale_code)
        json_file_path = os.path.join(metadata_dir, locale_dir_name, f"store-meta-{locale_dir_name}.json")

        if os.path.exists(json_file_path):
            with open(json_file_path, "r", encoding="utf-8") as json_file:
                json_data = json.load(json_file)

            title = json_data.get("title")
            full_description = json_data.get("full_description")
            short_description = json_data.get("short_description")

            if title:
                title_file_path = os.path.join(metadata_dir, locale_dir_name, "title.txt")
                with open(title_file_path, "w", encoding="utf-8") as title_file:
                    title_file.write(title)

            if full_description:
                full_description_file_path = os.path.join(metadata_dir, locale_dir_name, "full_description.txt")
                with open(full_description_file_path, "w", encoding="utf-8") as full_description_file:
                    full_description_file.write(full_description)

            if short_description:
                short_description_file_path = os.path.join(metadata_dir, locale_dir_name, "short_description.txt")
                with open(short_description_file_path, "w", encoding="utf-8") as short_description_file:
                    short_description_file.write(short_description)

# delete directories that doesn't contain any meta data
def clean_empty_dirs(locales, metadata_dir_name):
    print("cleanup")
    for locale in locales:
        metadata_locale_dir = map_android_locale_dir_to_metadata_dir(locale)
        json_file_path = os.path.join(metadata_dir_name, metadata_locale_dir, f"store-meta-{metadata_locale_dir}.json")
        if os.path.exists(json_file_path):
            remove = False
            with open(json_file_path, "r", encoding="utf-8") as json_file:
                json_data = json.load(json_file)
                # print(f"'{json_file_path}': '{json.dumps(json_data, sort_keys=True)}' ")

                if json.dumps(json_data, sort_keys=True) == "{}":
                    remove = True
                    # print(f"cleaning up empty json")

            if remove:
               # remove the empty json
               os.remove(json_file_path)
               print(f"removing empty json at '{json_file_path}'")

               # remove the directory if it's empty
               try:
                os.rmdir(os.path.join(metadata_dir_name, metadata_locale_dir))
                print(f"Removed dir '{os.path.join(metadata_dir_name, metadata_locale_dir)}'")
               except OSError:
                print(f"Skipped removal of dir '{os.path.join(metadata_dir_name, metadata_locale_dir)}'. Not empty.")



if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Pull translations from transifex for app and app store localization.')
    parser.add_argument('app_type', choices=['main', 'custom'], help='Type of the app (main or custom)')
    args = parser.parse_args()

    if args.app_type == "main":
        metadata_dir = get_script_path() + "/../src/normal/fastlane/metadata/android"
    elif args.app_type == "custom":
        metadata_dir = get_script_path() + "/../src/custom/fastlane/metadata/android"
    else:
        raise ValueError("Invalid app type. Use 'main' or 'custom'.")

    locales_list = list_locales(args.app_type)
    if not locales_list:
        raise ValueError(f"No valid locales found in the '{args.app_type}' app's 'res' directory.")

    # create empty meta data files in case they don't exist yet
    create_metadata_files(locales_list, args.app_type)
    print(f"Empty JSON files created for each locale in the '{args.app_type}' app.")

    # pull from transifex
    command = "tx pull -af"
    subprocess.run(command, shell = True, executable="/bin/bash")

    # parse the meta data jsons and create separate files, compatible with the fastlane file scheme
    split_json_and_save(locales_list, metadata_dir)
    print(f"JSON data split and saved to separate files for each locale in the '{args.app_type}' app.")

    # remove directories that doesn't contain any localizations or other fastlane meta data files
    clean_empty_dirs(locales_list, metadata_dir)
