# Updating l10n, metadata, f-droid, google play store

# F-Droid compatible fastlane metadata directory

This source folder only contains the generated metadata for f-droid builds for both Bitmask and a custom flavored client.
Currently neither F-Droid nor Fastlane support metadata dirs directly within build flavor dirs of a module, like 
`/<module>/src/<buildFlavor>/fastlane/metadata/android/` (which would be preferable). 
Transifex wants 1 file, play store wants 1 file, fastlane uses text files.

Keep an eye on this [issue](https://gitlab.com/fdroid/fdroidserver/-/issues/829) to track the state of the fastlane improvements for F-Droid.

## Updating l10n'ed app store listings

1. Fetch content from google play (en_US) with scripts/fetch-play-metadata.py
1. Check if there are changes with what fastlane creates in src/normal/fastlane/metadata/.. 
1. Use scripts/prepareForTx.py to check what localized app versions exist in the Android app, prepare for transifex upload
1. tx push se.leap.riseupvpn-desc.json -l en
1. wait for localization, answer questions, fix wording. Repeat when necessary.
1. Pull from transifex: tx pull -f --keep-new-files
1. prepare for upload and store digestion: scripts/prepareForTx.py
1. use fastlane to push to the google store

## Notes:

Translations that aren't completed (enough), won't be downloaded. Configure in .tx/config
The scripts create empty json files, because transifex needs them to even check.
Keep your API tokens at hand.
You need the tx cli client from transifex, pyton3, ptyhon3-babel

## How to update FROM the google play store:

Install fastlane:
https://docs.fastlane.tools/getting-started/android/setup/
update bundle

This will fetch the existing metadata. If you updated something through your browser, you can fetch that. There's no 'merge' functionality!
You can also use this when adding a new provider.

You need an API token from the Google Play store. If you don't have one yet, we have some links below that should help you get started.
fastlane supply init -j <YOUR-API-DETAILS.json> -p se.leap.<PROJECTNAME> -m src/custom/fastlane/metadata/

apt install pythong3-babel


## Getting API token

You need a Google cloud platform account, create a service account, grant relevant permissions, connect it with the play store.
https://medium.com/@Codeible/generating-the-json-web-token-for-the-google-play-developer-api-f6be6439b1af
https://developers.google.com/android-publisher/authorization

