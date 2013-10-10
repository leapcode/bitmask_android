#!/bin/bash

if [ -z "$2" ]
then
	echo Usage: debug.sh \"avd name\" \"project folder\"
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
adb shell am start -D se.leap.bitmaskclient/.Dashboard # Run app
sleep 1
pid=`adb shell ps | grep leap | awk '{print $2}'` # Identify the process id (pid) of the current bitmaskclient process instance
echo forwarding tcp:$localport to jwdp:$pid
adb forward tcp:$localport jdwp:$pid
jdb -sourcepath $PROJECT_FOLDER/src/ -attach localhost:$localport
