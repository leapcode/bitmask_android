# Updating l10n, metadata, f-droid, google play store

Easy to get confused, app strings, store strings, transifex, play store…

# Update app strings

Any string that will be changed in the source language (en_US) in the  master branch, will be picked up by transifex. Translators get notified about source string changes, best to put many changes at once. Notify translators, beta test users and localization lab about an upcoming release and request contributions.

# Metadata: F-Droid compatible fastlane metadata directory

The src/<flavor>/fastlande/metadata folder contains the generated metadata for f-droid builds for both Bitmask and a custom flavored client.
Currently neither F-Droid nor Fastlane support metadata dirs directly within build flavor dirs of a module, like 
`/<module>/src/<buildFlavor>/fastlane/metadata/android/` (which would be preferable). 
Transifex wants 1 file, play store wants 1 file, fastlane uses text files.

Keep an eye on this [issue](https://gitlab.com/fdroid/fdroidserver/-/issues/829) to track the state of the fastlane improvements for F-Droid.

## Updating localized app store listings

1. Fetch content from google play (en_US) with scripts/fetch-play-metadata.py
1. Check if there are changes with what fastlane creates in src/normal/fastlane/metadata/.. 
1. Now is a good moment to do a round of `APP` translation pulls. The
scripts/pulltranslations.py checks which translations for the app exist, to make sure
that we don't push metadata translations to the play store without having the app
translated.
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

