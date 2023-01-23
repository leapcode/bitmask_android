fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the tests

### android build_bitmask_for_screengrab

```sh
[bundle exec] fastlane android build_bitmask_for_screengrab
```

Build debug and test APK for screenshots

### android build_custom_for_screengrab

```sh
[bundle exec] fastlane android build_custom_for_screengrab
```

Build debug and test APK for screenshots

### android bitmask_screenshots

```sh
[bundle exec] fastlane android bitmask_screenshots
```



### android custom_build_screenshots

```sh
[bundle exec] fastlane android custom_build_screenshots
```



### android beta

```sh
[bundle exec] fastlane android beta
```

Submit a new Beta Build to Crashlytics Beta

### android deploy

```sh
[bundle exec] fastlane android deploy
```

Deploy a new version to the Google Play

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
