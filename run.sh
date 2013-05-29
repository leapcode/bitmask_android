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

echo "Press \"y\" key and enter if you want to wipe emulator's data"
read wipe_data_or_not
if [ $wipe_data_or_not == "y" ]
then
	echo "Wiping data"
	emulator -wipe-data @$avd_name & # If you want to test the app from scratch
else
	echo "Not wiping data"
	emulator @$avd_name & # If you want to test the app from scratch
fi

wait_until_booted
adb install -r $PROJECT_FOLDER/bin/LEAP\ Android-debug.apk # Install the new version of the application
adb shell am start se.leap.leapclient/.Dashboard # Run app
