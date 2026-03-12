# Updating l10n, metadata, f-droid, google play store

Easy to get confused, app strings, store strings, weblate, play store…

# Update app strings

Any string that appear in the master branch, will create a merge request in the
localization branch at
https://localizationlab.weblate.cloud/projects/bitmask/bitmask-android/ . Translators get
notified about source string changes in weblate. The reasoning: the CI only creates a
merge request when the other build pieces work (think of broken strings &$\). The merge request nudges another pair of
eyes to have a second look before publishing to weblate. Because there usually isn't a
very tight release schedule or only a short time window before release, this avoids too
many untranslated strings. A beta release can also have new strings and give feedback.

# Metadata: F-Droid compatible fastlane metadata directory

The src/<flavor>/fastlane/metadata folder contains the generated metadata for f-droid builds for both Bitmask and a custom flavored client.
Currently neither F-Droid nor Fastlane support metadata dirs directly within build flavor dirs of a module, like 
`/<module>/src/<buildFlavor>/fastlane/metadata/android/` (which would be preferable). 
Transifex wants 1 file, play store wants 1 file, fastlane uses text files.

Keep an eye on this [issue](https://gitlab.com/fdroid/fdroidserver/-/issues/829) to track the state of the fastlane improvements for F-Droid.

## Updating localized app store listings

1. Fetch content from google play (en_US) with scripts/fetch-play-metadata.py
1. Check if there are changes with what fastlane creates in src/normal/fastlane/metadata/.. 
1. Check out the translated metadata: `git clone -b bitmask-playstore https://0xacab.org/leap/l10n.git (the other branches for custom builds)
1. When there were any changes on weblate, they should be at https://0xacab.org/leap/l10n/-/tree/bitmask-playstore?ref_type=heads (or the RiseupVPN, for flavors). Maybe still as a merge request. Reach out to translators if there are important strings missing.
1. You want to double check: if the app is only half translated, you probably do not want to push an app store localization.
1. Pull store metadata translations from the l10n git repo scripts/pullTranslations.py main (or custom flavor)


1. Pull translations to this repository, from your earlier git clones: 

1. prepare for upload and store digestion: scripts/prepareForTx.py
1. use fastlane to push to the google store

## Notes:

Translations that aren't completed (enough), won't be integrated.
When there are new strings in this Android project at ../app/src/main/res/values/strings.xml in the main branch,
they will be updated in the bitmask branch here: https://0xacab.org/leap/l10n/-/tree/bitmask?ref_type=heads
Webhooks are configured to have weblate updating https://localizationlab.weblate.cloud/projects/bitmask/bitmask-android/

## How to update FROM the google play store:

Install fastlane:
https://docs.fastlane.tools/getting-started/android/setup/
update bundle

This will fetch the existing metadata. If you updated something through your browser, you can fetch that. There's no 'merge' functionality!
You can also use this when adding a new provider.

You need an API token from the Google Play store. If you don't have one yet, we have some links below that should help you get started.
fastlane supply init -j <YOUR-API-DETAILS.json> -p se.leap.<PROJECTNAME> -m src/customProductionFat/fastlane/metadata/

apt install pythong3-babel


## Getting API token

You need a Google cloud platform account, create a service account, grant relevant permissions, connect it with the play store.
https://medium.com/@Codeible/generating-the-json-web-token-for-the-google-play-developer-api-f6be6439b1af
https://developers.google.com/android-publisher/authorization

