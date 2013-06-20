#!/bin/bash

if [ -z "$1" ]
then
	echo Usage: prepareTestProviderAPI.sh \"project folder\"
	exit 0;
fi

PROJECT_FOLDER=$1
hosts_file="hosts-for-tests"

adb shell mount -o rw,remount -t yaffs2 /dev/block/mtdblock3 /system
adb push $PROJECT_FOLDER/$hosts_file /system/etc/hosts

echo "Pushed $PROJECT_FOLDER/$hosts_file"
