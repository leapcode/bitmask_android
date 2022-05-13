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

You have a couple options. The fist is more robust:

1. Insert a line in your `~/.bashrc` to automatically navigate to the correct directory (and back) whenever you invoke `emulator`:

 ```shell
function emulator { pushd `pwd`; cd "$(dirname "$(which emulator)")" && ./emulator "$@"; popd;}
```

2. Always run emulator from within its own directory (clunky!):

``` shell
 cd "$(dirname "$(which emulator)")"
 emulator <name_of_your_emulator>
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

If you need to refresh our upstream dependency on ics-openvpn, you may do so with:

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
