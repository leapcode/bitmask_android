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
* [Running Tests](#running-tests)
* [Debugging in an Emulator](#debugging-in-an-emulator)
  * [From Android Studio](#from-android-studio)
  * [From The Shell](#from-the-shell)
  * [Debian Gotchas](#debian-gotchas)
    * [Virtualization Not Enabled](#virtualization-not-enabled)
    * [Unpatched Filepaths Bug](#unpatched-filepaths-bug)
    * [Outdated GL Libraries](#outdated-gl-libraries)
* [Updating Submodules](#updating-submodules)
* [Acknowledgments](#acknowledgments)
* [Contributing](#contributing)

## License <a name="license"></a>

* [See LICENSE file](https://github.com/leapcode/bitmask_android/blob/master/LICENSE.txt)


## Installing <a name="installing"></a>

We will assume for convenience that you are installing on a Debian- or Ubuntu-based GNU/Linux machine. (Patches welcome with instructions for Mac, Windows, or other GNU/Linux distributions!)

The Bitmask Android Client has the following system-level dependencies:

* JDK v. 1.8
* Assorted 32-bit C libraries
* Android SDK Tools, v. 27.0.3, with these packages:
  * Platform-Tools, v. 27.0.3
  * Build-Tools, API v. 23-27
  * Platforms 23-27
  * Android Support Repository
  * Google Support Repository
  * NDK v. r16b (enables C code in Android)
* For running the app in an emulator, you will also need these packages:
  * Android Emulator
  * System Images for Android APIs 23-27
* The ICS-OpenVpn submodule

You can install them as follows:

### JDK <a name="jdk"></a>

Install with:

```bash
sudo apt install default-jdk
```

### C Libraries <a name="c-libraries"></a>

These are necessary to make sure the program cross-compiles to 32-bit architectures successfully from 64-bit GNU/Linux machines. If you don't have the lib32stdc++, try for example lib32stdc++-8-dev-x32-cross or whatever version is current on your system.

```
sudo apt install make gcc file lib32stdc++ lib32z1
```

### Android SDK <a name="android-sdk"></a>

#### With Android Studio <a name="with-android-studio"></a>

All of the Android SDK and NDK packages are downloadable through Android Studio, which (sadly) is probably the most hassle-free way to go about things.

You can download Android studio here:

https://developer.android.com/studio/index.html

Once you've got it installed, use the `SDK Manager` tool (Android figure Icon with blue arrow second from the right in the tool pane) to download all the Android SDK and NDK depencencies listed above.

#### With Bash <a name="with-bash"></a>

Alternatively (eg: for build machines), you may download and unzip the `android-sdk` bundle from Google as follows (assuming an install location of `/opt/android-sdk-linux`:

```
curl -L https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip -o sdk-tools.zip  \
    && unzip -q sdk-tools.zip -d /opt/android-sdk-linux \
    && rm -f sdk-tools.zip
```

To download the NDK (for cross-compiling and running the C code used in `ics-openvpn`), use:

```
curl -L http://dl.google.com/android/repository/android-ndk-r16b-linux-x86_64.zip -o ndk.zip  \
    && unzip ndk.zip -d /opt/android-sdk-linux/android-ndk-r16b \
    && rm -rf ndk.zip
```

After updating your PATH (see next step), you may now use the `sdkmanager` tool bundled with `android-sdk` to browse and install new sdk packages from Google.

To browse all available packages, run:

```shell
sdkmanager --list
```

To search for available packages of a certain type (eg: `tools`), run:

```shell
sdkmanager --list | grep tools
```

To install all of the dependencies listed above (targetting SDK versions 23 - 26), run:

```shell
sdkmanager tools
sdkmanager platform-tools
sdkmanager extras;android;m2repository
sdkmanager extras;google;m2repository
sdkmanager build-tools;27.0.3
sdkmanager build-tools;25.0.2
sdkmanager build-tools;23.0.3
sdkmanager platforms;android-27
sdkmanager platforms;android-25
sdkmanager platforms;android-23
```

#### Updating Your Path <a name="updating-your-path"></a>

Once you've installed Android SDK & NDK packages, you need to modify your PATH so you can invoke all the programs you just installed. You can do that with something like the following in your `~/.shellrc` or `~/.bash_profile`:

```shell
export ANDROID_HOME=<path/where/you/installed/android/sdk>
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk-bundle
export PATH=$ANDROID_NDK_HOME:$PATH
export PATH=$ANDROID_HOME/platform-tools:$PATH
export PATH=$ANDROID_HOME/tools/bin:$PATH
```

NOTE: On GNU/Linux machines, Android Studio installs the Android SDK in `~/Android/Sdk/`. Our dockerfile installs it in `/opt/android-sdk-linux`. You can install it wherever you want! Just be sure to remember where so you can add it to your PATH! :)

#### With Docker <a name="with-docker"></a>

Geesh! If all that above seems like a lot, it is!

To keep ourselves from messing it up all the time everyone someone new joins the project, we made a Dockerfile that creates the above environment with one line. You can pull the image and run builds from inside it, or consult the [Dockerfile](/docker/android-sdk.dockerfile) itself for requirements that your system might need but be missing.

Assuming you've already [installed docker](https://docs.docker.com/engine/installation/), you can pull the image with:

``` shell
docker pull 0xacab.org:4567/leap/bitmask_android/android-ndk:latest
```

Run the image with:

``` shell
docker run --rm -it 0xacab.org:4567/leap/bitmask_android/android-ndk:latest
```
More likely than not, you'll want to run the image with the source code mounted. You can do that with:

``` shell
cd <path/to/bitmask_android>
docker run --rm -it -v`pwd`:/bitmask_android -t 0xacab.org:4567/leap/bitmask_android/android-ndk:latest
```


### Submodules <a name="submodules"></a>

We depend on [ics-openvpn](https://github.com/schwabe/ics-openvpn) as an interface to Android's OpenVPN implementation. We include it as a [git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules) in our project, which requires that we initialize and update it (and its respective upstream submodule dependencies) in order to compile and run Bitmask Android.

We do so with:

```bash
cd <path/to/bitmask_android>
git submodule init
git submodule update --init --recursive
```

## Compiling <a name="compiling"></a>

You have lots of options for compiling, all of which will output Android-executable `apk` packages to `/bitmask_android/app/build/outputs/apk/`.

### Just Build It! <a name="just-build-it"></a>

If you compile the project for the first time you'll have to compile the dependencies. This can be done with:

```
./build_deps.sh
```
This command will create all libs we need for Bitmask.
 
If you want to to have a clean build of all submodules run
```
./cleanProject.sh
```
before you call `./build_deps.sh`. That script removes all build files and does the git submodule init and update job for you.  

You are then welcome to run:

```
./gradlew build
```

This will compile the code and run the tests, but not output any `apk` packages. As such, it's not all that useful. :)

### Debug APKs <a name="debug-apks"></a>

To assemble debug packages for running locally or testing in CI, run:

```bash
./build_deps.sh
./gradlew assembleDebug
```

This will output `app-insecure-debug.apk` and `app-production-debug.apk` to `/bitmask_android/app/build/outputs/apk/`.

### Release APKs <a name="release-apks"></a>

To assemble release packages, run:

```bash
./build_deps.sh
./gradlew assembleRelease
```

This will output `app-insecure-release.apk` and `app-production-release.apk` to `/bitmask_android/app/build/outputs/apk/`.

### Signed Release APKs <a name="signed-release-apks"></a>

If you want to release a signed APK (which you *must* do to publish the app to the Google Play store), you'll have to create a gradle.properties file in the project root with the following structure:

```properties
storeFileProperty=<fullPath>
storePasswordProperty=<store password without quotation marks>
keyAliasProperty=<key alias without quotation marks>
keyPasswordProperty=<key password without quotation marks>
```

### Building In Docker <a name="building-in-docker"></a>

If you want to make sure the environment you use to build APKs matches exactly the environment that Gitlab will use to build and publish artifacts, you can run any of the above build commands from inside Docker. To assemble a release build this way, run the following commands:

``` shell
$ cd <path/to/bitmask_android>
$ sudo docker run --rm -it -v `pwd`:/bitmask_android 0xacab.org:4567/leap/bitmask_android/android-ndk:latest
# cd /bitmask_android
# ./cleanProject.sh
# ./build_deps.sh
# ./gradlew assembleRelease
```

## Running Tests <a name="running-tests"></a>

To run the automated tests:

   1. Run an emulator
   2. Unlock Android
   3. Issue the command ./gradlew connectedCheck
   4. Pay attention and check the "Trust this app" checkbox, if you don't do so tests won't run.


## Debugging in an Emulator <a name="debugging-in-an-emulator"></a>

You can run the app in an emulator running any version of Android and simulating (almost) any device. To run it you'll have to create an emulator, run the emulator, and then load an assembled APK of the app onto the emulator. (You can then use all sort of nifty tools in [Anroid Debug Bridge](https://developer.android.com/studio/command-line/adb.html) to tail the logs and debug the app.)

Assuming you've already tackled (or don't need to tackle) the [Debian Gotchas](#debian-gotchas) listed below, you can do that using either Android Studio or a bash shell as follows:

### From Android Studio <a name="from-android-studio"></a>

To create an emulator:

* Select `Tools/Android/AVD Manager` from the application menu
* Follow the instructions

To run a pre-existing emulator:

* Open the `AVD Manager` as above
* Press the "Play" button next to the emulator you want to run

To run the app:

* Ensure you have an emulator running
* Open the left-hand project pane (Meta-1 or Cmd-1, depending on your keybindings)
* Navigate to `bitmask_android/app/src/main/java/se/leap/bitmaskclient/StartActivity`
* Right-click over the `StartActivity` filename and click the `Run 'StartActivity'` option (or use Shift-Ctl-F10 or Shift-Ctl-R, depending on your keybindings)
* After you have done this once, you should be able to simply select `StartActivity` from the dropdown menu next to the big green arrow in the toolbar, then click the green arrow to run the app.

### From the Shell <a name="from-the-shell"></a>

To list the available avd images for creating an emulator:

``` shell
avdmanager list
```

To create an emulator:

``` shell
avdmanager create avd
```

To list the emulators you have already created:

``` shell
avdmanager list avd
```

To run a pre-existing emulator called `Nexus_5_API_25`:

``` shell
emulator @Nexus_5_API_15
```

Verify the device is running with:

``` shell
adb devices
```

You should see something like:

``` shell
List of devices attached
emulator-5554 device
```
Install APK with:

``` shell
abd install <path/to/your>.apk
```

Uninstall with:

``` shell
abd uninstall se.leap.bitmaskclient
```
Install with option to reinstall:

``` shell
abd install -r <path/to/your/apk>
```

### Debian Gotchas <a name="debian-gotchas"></a>

If you are running Debian on a 64-bit machine, your emulator will likely not work out of the gate. Test to see if this is the case by:

* first creating an emulator in Android Studio (with name, eg, `Nexus_5_API_25`)
* then running:
   ```shell
   cd ~/
   emulator @<name_of_your_emulator>
   ```
If you can launch an emulator, HUZZAH! If not, you likely have one of 3 problems:

#### 1. Virtualization Not Enabled <a name="virtualization-not-enabled"></a>

Boo! Try turning it on. The second half of [this article](https://docs.fedoraproject.org/en-US/Fedora/13/html/Virtualization_Guide/sect-Virtualization-Troubleshooting-Enabling_Intel_VT_and_AMD_V_virtualization_hardware_extensions_in_BIOS.html) is a decent enough guide.

#### 2. Unpatched Filepaths Bug <a name="unpatched-filepaths-bug"></a>

**Symptoms:** If you have this bug, you will see something like the following when you try to spin up an emulator:

``` shell
[140500439390016]:ERROR:./android/qt/qt_setup.cpp:28:Qt library not found at ../emulator/lib64/qt/lib
Could not launch '../emulator/qemu/linux-x86_64/qemu-system-i386': No such file or directory
```
As [documented here](https://stackoverflow.com/questions/42554337/cannot-launch-avd-in-emulatorqt-library-not-found), there is a standing bug in the version of `emulator` packaged for emulator that assumes it always runs from within the `$ANDROID_HOME/emulator` directory, and can thus safely use relative filepaths, when in fact this is almost never the case. (Cool bug!)

**Fixes:**

You have a couple options. The second is more robust:

1. Always run emulator from within its own directory (clunky!):

``` shell
 cd "$(dirname "$(which emulator)")"
 emulator <name_of_your_emulator>
```

2. Insert a line in your `~/.bashrc` to automatically navigate to the correct directory (and back) whenever you invoke `emulator`:

 ```shell
function emulator { pushd `pwd`; cd "$(dirname "$(which emulator)")" && ./emulator "$@"; popd;}
```

#### 3. Outdated GL Libraries <a name="outdated-gl-libraries"></a>

**Symptoms:** If you have this bug, you will see something like the following:

``` shell
libGL error: failed to load driver: swrast
X Error of failed request:  BadValue (integer parameter out of range for  operation)
# redacted incredibly long stack trace
```

As documented [here](http://stackoverflow.com/questions/36554322/cannot-start-emulator-in-android-studio-2-0), the current emulator package ships without outdated versions of LibGL libraries. To work around this:

1. Install modern GL libriaries with:

``` shell
sudo apt-get install mesa-utils
```

2. Ensure that `emulator` always uses the correct libraries by either:

  a. always calling `emulator` with the `-use-system-libs` flag, like so:

  ``` shell
  emulator -use-system-libs -avd Nexus_5_API_25
  ```
  b. adding the following line to your ~/.bashrc or ~/.bash_profile:

  ```shell
  export ANDROID_EMULATOR_USE_SYSTEM_LIBS=1
  ```

**Special Android Studio Debian Bonus Gotcha:**

Assuming you have made all the above fixes (great job!), to be able to launch emulators from Android Studio, you must either:

1. Use the environment variable solution above (option a), then *always* launch Android Studio from a bash shell with:

``` shell
studio
```

This means never using the desktop launcher. :(

2. If you want to use the desktop launcher:

  * You must *always* launch emulators from the terminal. :(
  * But: you can quickly access a terminal inside of Android Studio with `OPTION-F12`

## Updating Submodules <a name="updating-submodules"></a>

If you need to refresh of our upstream dependency on ics-openvpn, you may do so with:

``` shell
cd <path/to/bitmask_android>
./gradlew updateIcsOpenVpn
```

Alternately:

```shell
cd <path/to/bitmask_android>
cd ics-openvpn
git remote add upstream https://github.com/schwabe/ics-openvpn.git
git pull --rebase upstream master
```
A bunch of conflicts may arise. The guidelines are:

    1. Methods in HEAD (upstream) completely removed from Bitmask should be removed again (e.g. askPW)
    2. Sometimes, Dashboard.class is in Bitmask while in ics-openvpn it is replaced by MainActivity.class and other classes. Keep removing them to keep Dashboard.class in there.
    3. Some resources files are stripped from several entries. Remove them if possible (check the code we compile is not using anything else new).

## Acknowledgments <a name="acknowledgments"></a>

This project bases its work in [ics-openvpn project](https://code.google.com/p/ics-openvpn/).

## Reporting Bugs <a name="reporting-bugs"></a>
Please file bug tickets on our main [development platform](https://0xacab.org/leap/bitmask_android/issues). You can either create an account on 0xacab.org or simply login with your github.com or gitlab.com account to create new tickets.

## Contributing <a name="contributing"></a>

Please fork this repository and contribute back using [pull requests](https://0xacab.org/leap/bitmask_android/merge_requests).

Our preferred method for receiving translations is our [Transifex project](https://www.transifex.com/projects/p/bitmask-android).

Any contributions, large or small, major features, bug fixes, additional language translations, unit/integration tests are welcomed and appreciated but will be thoroughly reviewed and discussed.
