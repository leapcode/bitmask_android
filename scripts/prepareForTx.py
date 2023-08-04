#!/usr/bin/env python3
""
__author__ = "kwadronaut"
__copyright__ = "Copyright 2023, LEAP"
__license__ = "GPL3 or later3 or later3 or later"
__version__ = "1"

import os
import re
import argparse
import json

# Set the path to the res directory containing different language folders
main_res_dir = "../app/src/main/res"
custom_res_dir = "../app/src/custom/res"

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
    return locales

# Create empty JSON file for each locale metadata directory
# If there's no file, tx will skip the translations
def create_metadata_files(locales, app_type):
    if app_type == "main":
        metadata_dir = "../src/normal/fastlane/metadata"
    elif app_type == "custom":
        metadata_dir = "../src/custom/fastlane/metadata"
    else:
        raise ValueError("Invalid app type. Use 'main' or 'custom'.")

    for locale_code in locales:
        # Remove "values-" prefix from the locale directory name
        locale_dir_name = locale_code.replace("values-", "")
        file_path = os.path.join(metadata_dir, locale_dir_name, f"store-meta-{locale_dir_name}.json")
        if not os.path.exists(file_path):  # Check if the file already exists
            os.makedirs(os.path.dirname(file_path), exist_ok=True)
            with open(file_path, "w", encoding="utf-8") as file:
                file.write("{}")  # Write an empty JSON object to the file

# Split JSON data and save to separate files for each locale
def split_json_and_save(locales, metadata_dir):
    for locale_code in locales:
        locale_dir_name = locale_code.replace("values-", "")
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

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Create metadata directories and empty JSON files for different locales.')
    parser.add_argument('app_type', choices=['main', 'custom'], help='Type of the app (main or custom)')
    args = parser.parse_args()

    if args.app_type == "main":
        metadata_dir = "../src/normal/fastlane/metadata"
    elif args.app_type == "custom":
        metadata_dir = "../src/custom/fastlane/metadata"
    else:
        raise ValueError("Invalid app type. Use 'main' or 'custom'.")

    locales_list = list_locales(args.app_type)
    if locales_list:
        print("List of Locales:")
        for locale_code in locales_list:
            print(locale_code)

        create_metadata_files(locales_list, args.app_type)
        print(f"Empty JSON files created for each locale in the '{args.app_type}' app.")

        split_json_and_save(locales_list, metadata_dir)
        print(f"JSON data split and saved to separate files for each locale in the '{args.app_type}' app.")
    else:
        print(f"No valid locales found in the '{args.app_type}' app's 'res' directory.")

