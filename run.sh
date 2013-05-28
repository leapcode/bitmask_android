#!/bin/bash

if [ -z "$2" ]
then
	echo Usage: run.sh \"avd name\" \"project folder\"
	exit 0;
fi
avd_name=$1
PROJECT_FOLDER=$2
localport=`expr $RANDOM % 65536`

wait_until_booted() {
	OUT=`adb shell getprop init.svc.bootanim`
	RES="stopped"

	while [[ ${OUT:0:7}  != 'stopped' ]]; do
		OUT=`adb shell getprop init.svc.bootanim`
#		echo 'Waiting for emulator to fully boot...'
		sleep 5
	done

	echo "Emulator booted!"
}

emulator -wipe-data @$avd_name & # If you want to test the app from scratch
wait_until_booted
adb install -r $PROJECT_FOLDER/bin/LEAP\ Android-debug.apk # Install the new version of the application
adb shell am start se.leap.leapclient/.Dashboard # Run app
