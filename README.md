# Bitmask Android App 

This repository contains the source code for the [Bitmask](https://bitmask.net/) Android app.

Please see the [issues](https://github.com/leapcode/bitmask_android/issues) section to
report any bugs or feature requests and to see the list of known issues.

## License

* [See LICENSE file](https://github.com/leapcode/bitmask_android/blob/master/LICENSE.txt)

## Build Requirements

Install from developer.android.com:

* Android SDK, API 17: http://developer.android.com/sdk/index.html
* Android NDK, r9d: http://developer.android.com/tools/sdk/ndk/index.html

Make sure add the necessary android tools to your bin path. For example, assuming you installed
the SDK and NDK to `~/dev` on a linux machine, you would add this to your path:

    ~/dev/android-sdk-linux/tools
    ~/dev/android-sdk-linux/platform-tools
    ~/dev/android-ndk-r9d

Installable via `android` command (SDK Manager):

* Android SDK Build-tools, 19.0.3
* Android Support Repository, 4+

Finally, install a java compiler. For example:

   sudo apt-get install default-jdk

## Update git submodules

We build upon ics-openvpn, which meets a submodule in our project structure.

For that reason, it is necessary to initialize and update them before being able to build Bitmask Android.

    git submodule init
    git submodule update
    cd ics-openvpn
    git submodule init
    git submodule update

## Build native sources

To build NDK sources, you need to issue these commands:

    cd app
    ./build-native.sh
    cd .. (to get back to the project directory)

### Compiling from the command line
#### Signed APK

If you want to release a signed APK, you'll have to create a gradle.properties file in the project root with the following structure:
    
    storeFileProperty=fullPath
    storePasswordProperty=store password without quotation marks
    keyAliasProperty=key alias without quotation marks
    keyPasswordProperty=key password without quotation marks
    
#### Actual command
    ./gradlew build

The resulting apk(s) will be in `app/build/apk`.

### Using Android Studio

* `Import project` => select bitmask_android top folder

## Running tests

To run the automated tests:
   1. Run an emulator (device doesn't necesarily has root, so testVpnCertificateValidator.testIsValid may fail).
   2. Unlock Android
   3. Issue the command ./gradlew connectedCheck
   4. Pay attention and check the "Trust this app" checkbox, if you don't do so tests won't run.

Due to the nature of some tests, adb will lose its connectivity and you won't receive any tests results. To look for failed tests, do the following:
   1. adb kill-server
   2. adb logcat | less
   3. Look for: "failed: test"

We'll polish this process soon, but right now that's what we're doing (well, in fact, we run "adb logcat" in Emacs and then search "failed: test" in the corresponding buffer ;) ).

## Acknowledgements

This project bases its work in [ics-openvpn project](https://code.google.com/p/ics-openvpn/).

## Contributing

Please fork this repository and contribute back using
[pull requests](https://github.com/leapcode/leap_android/pulls).

Our preferred method for receiving translations is our [Transifex project](https://www.transifex.com/projects/p/bitmask-android).

Any contributions, large or small, major features, bug fixes, additional
language translations, unit/integration tests are welcomed and appreciated
but will be thoroughly reviewed and discussed.
