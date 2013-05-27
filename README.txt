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
2. android update project --path $PROJECT_LOCATION/leap_android/
3. ant debug

Postconditions
--------------

1. $PROJECT_LOCATION/leap_android/bin/LEAP Android-debug.apk exists

Running on the emulator
=========================

Preconditions
-----------------

1. Android SDK is installed, and its tools are in the PATH.
2. leap_android has been compiled.
3. An avd exists in ~/.android/avd/ (if you do not have one, follow instructions from http://developer.android.com/tools/devices/managing-avds-cmdline.html)

Instructions to run on the emulator
-----------------------------------

1. Run emulator: emulator @AVD-NAME (avd names are the names of the files in ~/.android/avd with extension .avd).
	- If you want to test the app from scratch, run emulator -wipe-data @AVD-NAME
2. Run app: adb shell am start se.leap.leapclient/.Dashboard

Postconditions
--------------

1. LEAP Android is running.

Debugging from console
======================

Preconditions
-----------------

1. Android SDK is installed, and its tools are in the PATH.
2. leap_android has been compiled.
3. An avd exists in ~/.android/avd/ (if you do not have one, follow instructions from http://developer.android.com/tools/devices/managing-avds-cmdline.html).
4. jdb is installed (this program is part of OpenJDK 7)

Instructions to debug from the console
-----------------------------------

1. emulator @AVD-NAME # (avd names are the names of the files in ~/.android/avd with extension .avd).
	- emulator -wipe-data @AVD-NAME # If you want to test the app from scratch
2. adb install -r $PROJECT_LOCATION/leap_android/bin/LEAP\ Android-debug.apk # Install the new version of the application
3. adb shell am start -D se.leap.leapclient/.Dashboard # Run app
4. pid=`adb shell ps | grep leap | awk '{print $2}'` # Identify the process id (pid) of the current leapclient process instance
5. localport=`expr $RANDOM % 65536`
6. adb forward tcp:$localport jdwp:$pid
7. jdb -sourcepath $PROJECT_LOCATION/leap_android/src/ -attach localhost:$localport

Postconditions
--------------

1. LEAP Android is running.
2. LEAP Android does not show the message "Application LEAP for Android (process se.leap.leapclient) is waiting for the debugger to attach".
