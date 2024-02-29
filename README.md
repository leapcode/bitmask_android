# Bitmask Android Client

This repository contains the source code for the [Bitmask](https://bitmask.net/) Android client. Bitmask Android offers one-click free VPN service from trusted providers of the LEAP stack.

To learn about the stack, visit [leap.se](https://leap.se).

Please see the [issues](https://0xacab.org/leap/bitmask_android/issues) section to report any bugs or feature requests, and to see the list of known issues.

# Table Of Contents

* [License](#license)
* [Installing](#installing)
  * [JDK](#jdk)
  * [C Libraries](#c-libraries)
  * [Android SDK](#android-sdk)
    * [With Android Studio](#with-android-studio)
    * [With Bash](#with-bash)
    * [Updating Your PATH](#updating-your-path)
    * [With Docker](#with-docker)
  * [Submodules](#submodules)
* [Compiling](#compiling)
  * [Just Build It!](#just-build-it)
  * [Debug APKs](#debug-apks)
  * [Release APKs](#release-apks)
  * [Signed Release APKs](#signed-release-apks)
  * [Localization](#localize)
* [Supported Versions](#supported-versions)
* [Acknowledgments](#acknowledgments)
* [Contributing](#contributing)

## License <a name="license"></a>

* [See LICENSE file](https://github.com/leapcode/bitmask_android/blob/master/LICENSE.txt)


## Installing <a name="installing"></a>

We will assume for convenience that you are installing on a Debian- or Ubuntu-based GNU/Linux machine. (Patches welcome with instructions for Mac, Windows, or other GNU/Linux distributions!)

The Bitmask Android Client has the following system-level dependencies:

* JDK 17
* Android SDK Tools, v. 34.0.3, with these packages:
  * Platform-Tools, v. 34.0.5
  * Build-Tools, API v. 34
  * Platforms 34
  * Android Support Repository
  * Google Support Repository
  * NDK v. r21e (enables C code in Android)
* For running the app in an emulator, you will also need these packages:
  * Android Emulator
  * System Images for Android APIs 30
* ics-openvpn submodule
* tor-android submodule
* bitmaskcore submodule

You can install them as follows:

### JDK <a name="jdk"></a>

Install with:

```bash
sudo apt-get update -qq && \
    apt-get install -y openjdk-17-jdk
```

### C Libraries <a name="c-libraries"></a>

These are necessary to make sure the program cross-compiles openssl, openvpn, tor etc. for Bitmask Android.

```
sudo apt-get -y install make gcc swig file lib32stdc++6 lib32z1 autoconf autogen automake autopoint autotools-dev gettext-base libtool patch pkg-config mesa-utils
```

### Android SDK <a name="android-sdk"></a>

#### With Android Studio <a name="with-android-studio"></a>

All of the Android SDK and NDK packages are downloadable through Android Studio, which (sadly) is probably the most hassle-free way to go about things.

You can download Android studio here:

https://developer.android.com/studio/index.html

Once you've got it installed, use the `SDK Manager` tool (Android figure Icon with blue arrow second from the right in the tool pane) to download all the Android SDK and NDK dependencies listed above.

#### With Bash <a name="with-bash"></a>

Alternatively (eg: for build machines), you may have a look at our docker build files in [the docker directory](/docker/)

#### Updating Your Path <a name="updating-your-path"></a>

Once you've installed Android SDK & NDK packages, you need to modify your PATH so you can invoke all the programs you just installed. You can do that with something like the following in your `~/.shellrc` or `~/.bash_profile`:

```shell
export ANDROID_HOME=<path/where/you/installed/android/sdk>
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/21.4.7075529
export PATH=$ANDROID_NDK_HOME:$PATH
export PATH=$ANDROID_HOME/platform-tools:$PATH
export PATH=$ANDROID_HOME/tools/bin:$PATH
```

#### With Docker <a name="with-docker"></a>

Geesh! If all that above seems like a lot, it is!

To keep ourselves from messing it up all the time someone new joins the project, we made a Dockerfile that creates the above environment with one line. You can pull the image and run builds from inside it, or consult [android-sdk Dockerfile](/docker/android-sdk/Dockerfile) and [android-ndk Dockerfile](/docker/android-ndk/Dockerfile) itself for requirements that your system might need but be missing.

Assuming you've already [installed docker](https://docs.docker.com/engine/installation/), you can pull the image with:

``` shell
docker pull registry.0xacab.org/leap/bitmask_android/android-ndk:latest
```

Run the image with:

``` shell
docker run --rm -it registry.0xacab.org/leap/bitmask_android/android-ndk:latest
```
More likely than not, you'll want to run the image with the source code mounted. You can do that with:

``` shell
cd <path/to/bitmask_android>
docker run --rm -it -v`pwd`:/bitmask_android -t registry.0xacab.org/leap/bitmask_android/android-ndk:latest
```


### Submodules <a name="submodules"></a>

We depend on several [git submodules](https://git-scm.com/book/en/v2/Git-Tools-Submodules) to build Bitmask Android: 
* [ics-openvpn](https://github.com/schwabe/ics-openvpn) as an interface to Android's OpenVPN implementation.
* [bitmaskcore](https://0xacab.org/leap/android_libs/bitmaskcore.git) mainly as a library for Pluggable Transports (censorship circumvention functionality),
* [tor-android](https://0xacab.org/leap/android_libs/tor-android.git) to protect the communication to configuration servers from being blocked.
In order to initialize and update these submodules run:

```bash
cd <path/to/bitmask_android>
git submodule init
git submodule update --init --recursive
```

## Compiling <a name="compiling"></a>

### Just Build It! <a name="just-build-it"></a>

If you compile the project for the first time you'll have to compile the dependencies.
In order to get a clean build and fetch all submodules run:
```
./scripts/cleanProject.sh
```

The following command will create all libs we need for Bitmask.
```
./scripts/build_deps.sh
```
 
If you run into errors, you may miss some dependencies on your system. Again, please check the `RUN apt-get` commands of [android-sdk Dockerfile](/docker/android-sdk/Dockerfile) and [android-ndk Dockerfile](/docker/android-ndk/Dockerfile) to get an idea what might be missing.
Please note that tor-android can only be build on Linux machines right now. Fixes for Windows and MacOS are more than welcome!
In order to temporarily disable building tor you can run:
```
BUILD_TOR=false ./scripts/build_deps.sh
```
### Custom Builds
Please refer to to the customization [README](/app/src/custom/README.md).

### Debug APKs <a name="debug-apks"></a>

After having run `./build_deps.sh`, you can assemble debug packages for running locally or testing in CI:
```bash
./gradlew assembleNormalProductionFatDebug
```

If everything went fine, you will find the debug apks in `/bitmask_android/app/build/outputs/apk/`.

### Release APKs <a name="release-apks"></a>

To build releases, a script comes to the rescue: [prepareForDistribution.sh](/scripts/prepareForDistribution.sh) `

Before you can actually build a release make sure you have setup a keystore with your Android signing key. Additionally you can sign your software with your PGP key using this script. 

If you want to build and sign apks and aab bundles for the current commit, run:
```bash
  ./scripts/prepareForDistribution.sh build sign -ks ~/path/to/bitmask-android.keystore -ka <yourKeystoreKeyAlias>
```

Please check `./prepareFordistribution.sh -h` for all options!

### Running unit tests

Unit tests should always run against release builds, it is expected that some tests fail in debug builds.

You can execute all unit tests from the command line with:

```bash
  ./gradlew testCustomProductionFatReleaseUnitTest testNormalProductionFatReleaseUnitTest
```

## Localization and translate <a name="Localization"></a>

All the localizations happen in Transifex. How to generate, push, pull the translations are detailed in src/README.md

## Supported Versions <a name="supported-versions"></a>

Currently API 21 (Android 5.0) - API 34 (Android 14) are officially supported. Keep backwards compatibility in mind if you plan to contribute new features.

## Acknowledgments <a name="acknowledgments"></a>

This project bases its work in [ics-openvpn project](https://code.google.com/p/ics-openvpn/).

## Reporting Bugs <a name="reporting-bugs"></a>
Please file bug tickets on our main [development platform](https://0xacab.org/leap/bitmask_android/issues). You can either create an account on 0xacab.org or simply login with your github.com or gitlab.com account to create new tickets.

## Contributing <a name="contributing"></a>

Please fork this repository and contribute back using [pull requests](https://0xacab.org/leap/bitmask_android/merge_requests).

Our preferred method for receiving translations is our [Transifex project](https://www.transifex.com/otf/bitmask). Localization Lab helps us with on-boarding volunteers. If you are new to translations and Transifex we've got a [short how-to](https://wiki.localizationlab.org/index.php/Bitmask).

Any contributions, large or small, major features, bug fixes, additional language translations, unit/integration tests are welcomed and appreciated but will be thoroughly reviewed and discussed.
