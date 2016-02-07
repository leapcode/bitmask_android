# Bitmask Android App 

This repository contains the source code for the [Bitmask](https://bitmask.net/) Android app.

Please see the [issues](https://github.com/leapcode/bitmask_android/issues) section to
report any bugs or feature requests and to see the list of known issues.

## License

* [See LICENSE file](https://github.com/leapcode/bitmask_android/blob/master/LICENSE.txt)

## Build Requirements

Install from developer.android.com:

* Android SDK, API 23: http://developer.android.com/sdk/index.html
* Android NDK, r10e: http://developer.android.com/tools/sdk/ndk/index.html

Make sure add the necessary android tools to your bin path. For example, assuming you installed
the SDK and NDK to `~/dev` on a linux machine, you would add this to your path:

    ~/dev/android-sdk-linux/tools
    ~/dev/android-sdk-linux/platform-tools
    ~/dev/android-ndk-r10e/

Installable via `android` command (SDK Manager):

* Android SDK Build-tools, 23.0.2
* Android Support Repository, 4+

Finally, install a java compiler. For example:

   sudo apt-get install default-jdk

If you are using a 64-bit machine, you will need to install some libraries too:
    sudo apt-get install lib32stdc++6 lib32z1

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

## Updating ics-openvpn

    cd ics-openvpn
    git remote add upstream https://github.com/schwabe/ics-openvpn.git
    git pull --rebase upstream master
    
A bunch of conflicts may arise. The guidelines are: 
    1. Methods in HEAD (upstream) completely removed from Bitmask should be removed again (e.g. askPW)
    2. Sometimes, Dashboard.class is in Bitmask while in ics-openvpn it is replaced by MainActivity.class and other classes. Keep removing them to keep Dashboard.class in there.
    3. Some resources files are stripped from several entries. Remove them if possible (check the code we compile is not using anything else new).

    ./gradlew updateIcsOpenVpn
## Acknowledgements

This project bases its work in [ics-openvpn project](https://code.google.com/p/ics-openvpn/).

## Contributing

Please fork this repository and contribute back using
[pull requests](https://github.com/leapcode/leap_android/pulls).

Our preferred method for receiving translations is our [Transifex project](https://www.transifex.com/projects/p/bitmask-android).

Any contributions, large or small, major features, bug fixes, additional
language translations, unit/integration tests are welcomed and appreciated
but will be thoroughly reviewed and discussed.
