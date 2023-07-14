# Updating l10n, metadata, f-droid, google play store
# F-Droid compatible fastlane metadata directory

This source folder only contains the generated metadata for f-droid builds for both Bitmask and a custom flavored client.
Currently neither F-Droid nor Fastlane support metadata dirs directly within build flavor dirs of a module, like 
`/<module>/src/<buildFlavor>/fastlane/metadata/android/` (which would be preferable). 

Keep an eye on this [issue](https://gitlab.com/fdroid/fdroidserver/-/issues/829) to track the state of the fastlane improvements for F-Droid.

#How to update and go forward:
Install fastlane:
https://docs.fastlane.tools/getting-started/ios/setup/
update bundle

This will fetch the existing metadata. If you updated something through your browser, you can fetch that. There's no 'merge' functionality!

fastlane supply init -j <YOUR-API-DETAILS.json> -p se.leap.<PROJECTNAME> -m src/custom/fastlane/metadata/
