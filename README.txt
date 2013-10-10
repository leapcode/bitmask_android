Compiling
=========

Preconditions
----------------

1. Android SDK installed (follow instructions from http://developer.android.com/sdk/index.html)
2. API version 16 or version installed.
2. Ant 1.6 or greater

Instructions to compile
-----------------------

1. cd $PROJECT_LOCATION/leap_android
2. ./compile.sh

Postconditions
--------------

1. $PROJECT_LOCATION/leap_android/bin/Bitmask Android-debug.apk exists

Running on the emulator
=========================

Preconditions
-----------------

1. Android SDK is installed, and its tools are in the PATH.
2. Bitmask Android has been compiled.
3. An avd exists in ~/.android/avd/ (if you do not have one, follow instructions from http://developer.android.com/tools/devices/managing-avds-cmdline.html)

Instructions to run on the emulator
-----------------------------------

1. cd $PROJECT_LOCATION/leap_android
1. Run script: ./run.sh @AVD-NAME . (avd names are the names of the files in ~/.android/avd with extension .avd).

Postconditions
--------------

1. Bitmask Android is running.

Debugging from console
======================

Preconditions
-----------------

1. Android SDK is installed, and its tools are in the PATH.
2. Bitmask Android has been compiled.
3. An avd exists in ~/.android/avd/ (if you do not have one, follow instructions from http://developer.android.com/tools/devices/managing-avds-cmdline.html).
4. jdb is installed (this program is part of OpenJDK 7)

Instructions to debug from the console
-----------------------------------

1. cd $PROJECT_LOCATION/leap_android
2. Run script: ./debug.sh @AVD-NAME . (avd names are the names of the files in ~/.android/avd with extension .avd).

Postconditions
--------------

1. Bitmask Android is running.
2. Bitmask Android does not show the message "Application Bitmask for Android (process se.leap.bitmaskclient) is waiting for the debugger to attach".
3. You are in a jdb debuggin session.
