import os
import argparse
import json
from googleapiclient.discovery import build
from google.oauth2 import service_account

# Load API key from environment variable
api_key = os.environ.get('GOOGLE_PLAY_API_KEY')

# Load command-line arguments
parser = argparse.ArgumentParser(description='Fetch app details from Google Play Store.')
parser.add_argument('package_name', help='Package name of the app')
parser.add_argument('--source-language', action='store_true', help='Fetch app description in source language')
parser.add_argument('--list-languages', action='store_true', help='List all supported languages')
parser.add_argument('--extract-details', action='store_true', help='Extract and save app details as JSON')
parser.add_argument('--extract-changelog', action='store_true', help='Extract and save changelog as JSON')
args = parser.parse_args()

# Create a service account credentials object
credentials = service_account.Credentials.from_service_account_file(
    '/home/kwadronaut/dev/leap/secrets/android-api.json',
    scopes=['https://www.googleapis.com/auth/androidpublisher']
)

# Build the service object for the Google Play Developer API
service = build('androidpublisher', 'v3', credentials=credentials, cache_discovery=False)

# Fetch app details
def fetch_app_details(package_name, language):
    # Create a new edit
    edit_request = service.edits().insert(body={}, packageName=package_name)
    edit_response = edit_request.execute()
    edit_id = edit_response['id']

    # Fetch the app listing for the specified language within the edit
    app_details = service.edits().listings().get(
        packageName=package_name,
        editId=edit_id,
        language=language
    ).execute()

    # Commit the edit (optional)
    service.edits().commit(
        packageName=package_name,
        editId=edit_id
    ).execute()

    return app_details, edit_id

# Fetch changelog for a specific version
def fetch_changelog(package_name, edit_id):
    # Fetch the tracks for the package
    tracks = service.edits().tracks().list(packageName=package_name, editId=edit_id).execute()
    track = tracks['tracks'][0]['track']

    # Fetch the changelog for the track
    changelog = service.edits().tracks().get(packageName=package_name, editId=edit_id, track=track).execute()

    return changelog

# Package name
package_name = args.package_name

# Fetch app details
try:
    #app_details, edit_id = fetch_app_details(package_name, 'en-US')
    app_details, edit_id = fetch_app_details(package_name, 'nl-NL')
    print("App Details:")
    print(app_details)
except Exception as e:
    print("An error occurred:", str(e))

# List all supported languages
if args.list_languages:
    try:
        # Create a new edit
        edit_request = service.edits().insert(body={}, packageName=package_name)
        edit_response = edit_request.execute()
        edit_id = edit_response['id']

        # Fetch the app listings for the edit
        listings = service.edits().listings().list(packageName=package_name, editId=edit_id).execute()
        supported_languages = [listing['language'] for listing in listings['listings']]
        print("Supported Languages:")
        for language in supported_languages:
            print(language)

        # Commit the edit
        service.edits().commit(packageName=package_name, editId=edit_id).execute()
    except Exception as e:
        print("An error occurred:", str(e))

# Extract and save app details as JSON
if args.extract_details:
    try:
        # Extract the text fields from app details
        title = app_details['title']
        full_description = app_details['fullDescription']
        short_description = app_details['shortDescription']

        # Create a dictionary to hold the extracted app details
        extracted_details = {
            'title': title,
            'full_description': full_description,
            'short_description': short_description
        }

        # Determine the output file path based on the language
        language = 'en-US'  # Update with the desired language
        json_file_path = f"locale/{language}/transifex.json"
        
        # Create the output directory if it doesn't exist
        os.makedirs(os.path.dirname(json_file_path), exist_ok=True)

        # Save the extracted details as JSON
        with open(json_file_path, 'w', encoding='utf-8') as json_file:
            json.dump(extracted_details, json_file, ensure_ascii=False, indent=4)

        print(f"App details extracted and saved to {json_file_path} as JSON successfully.")
    except Exception as e:
        print("An error occurred:", str(e))

# Extract and save changelog as JSON
if args.extract_changelog:
    try:
        changelog = fetch_changelog(package_name, edit_id)
        print("Changelog:")
        print(changelog)

        # Determine the output file path based on the language
        language = 'en-US'  # Update with the desired language
        json_file_path = f"locale/{language}/transifex.json"

        # Create the output directory if it doesn't exist
        os.makedirs(os.path.dirname(json_file_path), exist_ok=True)

        # Save the changelog as JSON
        with open(json_file_path, 'w', encoding='utf-8') as json_file:
            json.dump(changelog, json_file, ensure_ascii=False, indent=4)

        print(f"Changelog extracted and saved to {json_file_path} as JSON successfully.")
    except Exception as e:
        print("An error occurred:", str(e))

