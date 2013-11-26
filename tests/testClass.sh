#!/bin/bash
classfile=$1
classline=`grep " class" $classfile`
next=0
for word in $classline
do
	if [ $word == "class" ]
	then
		next=1
	elif [ $next -eq 1 ]
	then
		classname=$word
		next=0
	fi
done

adb shell am instrument -w -e class se.leap.bitmaskclient.test.$classname se.leap.bitmaskclient.test/android.test.InstrumentationTestRunner
