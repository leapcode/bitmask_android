#!/bin/bash

if [ -z "$2" ]
then
	echo Usage: debug.sh \"avd name\" \"project folder\"
	exit 0;
fi
avd_name=$1
PROJECT_FOLDER=$2
localport=`expr $RANDOM % 65536`

emulator -wipe-data @$avd_name & # If you want to test the app from scratch
sleep 70
adb install -r $PROJECT_FOLDER/bin/LEAP\ Android-debug.apk # Install the new version of the application
adb shell am start -D se.leap.leapclient/.Dashboard # Run app
pid=`adb shell ps | grep leap | awk '{print $2}'` # Identify the process id (pid) of the current leapclient process instance
adb forward tcp:$localport jdwp:$pid
sleep 3
jdb -sourcepath $PROJECT_FOLDER/src/ -attach localhost:$localport
