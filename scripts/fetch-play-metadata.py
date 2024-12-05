import os
import argparse
import json
from googleapiclient.discovery import build
from google.oauth2 import service_account

# Command-line arguments
parser = argparse.ArgumentParser(description='Fetch app details from Google Play Store.')
parser.add_argument('package_name', help='Package name of the app')
parser.add_argument('--language', default=None, help='Specify the language to fetch details (default: None, fetch all supported languages)')
parser.add_argument('--list-languages', action='store_true', help='List all supported languages')
parser.add_argument('--extract-details', action='store_true', help='Extract app details')
parser.add_argument('--extract-changelog', action='store_true', help='Extract and save changelog as JSON')
parser.add_argument('--save', action='store_true', help='Save the fetched details to the correct location')
parser.add_argument('--commit', action='store_true', help='Commit changes to the Play Store')
args = parser.parse_args()

# Get the credentials file path from an environment variable
credentials_path = os.environ.get('GOOGLE_PLAY_CREDENTIALS', None)
if not credentials_path:
    raise ValueError("Environment variable 'GOOGLE_PLAY_CREDENTIALS' is not set.")

# Create a service account credentials object
credentials = service_account.Credentials.from_service_account_file(
    credentials_path,
    scopes=['https://www.googleapis.com/auth/androidpublisher']
)
service = build('androidpublisher', 'v3', credentials=credentials, cache_discovery=False)

# Base path for Fastlane metadata
def get_metadata_path(package_name):
    if package_name == "se.leap.bitmaskclient":
        return 'src/normal/fastlane/metadata/android'
    elif package_name == "se.leap.riseupvpn":
        return 'src/custom/fastlane/metadata/android'
    else:
        raise ValueError(f"Unknown package name: {package_name}")

# Fetch app details
def fetch_app_details(package_name, language):
    try:
        # Create a new edit
        edit_request = service.edits().insert(body={}, packageName=package_name)
        edit_response = edit_request.execute()
        edit_id = edit_response['id']

        # Fetch the app listing for the specified language
        app_details = service.edits().listings().get(
            packageName=package_name,
            editId=edit_id,
            language=language
        ).execute()

        # Commit the edit if the --commit flag is provided
        if args.commit:
            service.edits().commit(
                packageName=package_name,
                editId=edit_id,
                changesNotSentForReview=True
            ).execute()

        return app_details
    except Exception as e:
        print(f"Error fetching app details: {str(e)}")
        return None

# Save app details in Fastlane structure
def save_app_details(app_details, package_name, language):
    try:
        # Determine the correct metadata path based on the package name
        metadata_path = get_metadata_path(package_name)
        lang_dir = os.path.join(metadata_path, language)
        os.makedirs(lang_dir, exist_ok=True)

        # Save `title`, `short_description`, and `full_description` as text files
        with open(os.path.join(lang_dir, 'title.txt'), 'w', encoding='utf-8') as title_file:
            title_file.write(app_details.get('title', ''))

        with open(os.path.join(lang_dir, 'short_description.txt'), 'w', encoding='utf-8') as short_file:
            short_file.write(app_details.get('shortDescription', ''))

        with open(os.path.join(lang_dir, 'full_description.txt'), 'w', encoding='utf-8') as full_file:
            full_file.write(app_details.get('fullDescription', ''))

        print(f"App details saved successfully in {lang_dir}")
    except Exception as e:
        print(f"Error saving app details: {str(e)}")

# List langs in the store
def list_supported_languages(package_name):
    try:
        # Create a new edit
        edit_request = service.edits().insert(body={}, packageName=package_name)
        edit_response = edit_request.execute()
        edit_id = edit_response['id']

        # Fetch the app listings
        listings = service.edits().listings().list(packageName=package_name, editId=edit_id).execute()
        supported_languages = [listing['language'] for listing in listings['listings']]
        print("Supported Languages:")
        for language in supported_languages:
            print(language)

        # Commit the edit if the --commit flag is provided
        if args.commit:
            service.edits().commit(
                packageName=package_name,
                editId=edit_id,
                changesNotSentForReview=True
            ).execute()

        return supported_languages
    except Exception as e:
        print(f"Error listing languages: {str(e)}")
        return []

# Main script logic
if args.list_languages:
    list_supported_languages(args.package_name)
elif args.extract_details:
    if args.save:
        # Fetch and save details for all supported languages
        supported_languages = list_supported_languages(args.package_name)
        for language in supported_languages:
            app_details = fetch_app_details(args.package_name, language)
            if app_details:
                save_app_details(app_details, args.package_name, language)
    elif args.language:
        # Fetch and save details for a specific language
        app_details = fetch_app_details(args.package_name, args.language)
        if app_details:
            if args.save:
                save_app_details(app_details, args.package_name, args.language)
            else:
                print("Fetched app details (not saved):")
                print(json.dumps(app_details, indent=4))
    else:
        print("Please specify a language with --language or use --list-languages to list supported languages.")
else:
    print("No valid action specified. Use --list-languages, --extract-details, or --extract-changelog.")
