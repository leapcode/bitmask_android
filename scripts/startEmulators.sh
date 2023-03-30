#!/bin/bash

PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/Sdk/tools:$ANDROID_HOME/emulator
apt-get update
apt-get install -y libpulse-java libpulse0 imagemagick libxkbcommon-x11-0 xvfb vulkan-tools
# there's a QT thing missing
emulator -accel-check
docker info
export DISPLAY=:99.0

# init parameters
for ((i=1;i<=$#;i++)); 
do
	if [[ ${!i} = "-n" ]]
	then
		((i++))
		N=${!i}
	elif [[ ${!i} = "-h" ]]
	then
		echo -e "
        	-n 	start first N available emulators from alphabetically order
		-h	print help
		"
		exit
	fi
done

if [[ -z ${N} ]]
then
	N=1
fi

err() {
        echo "$@"
        exit 1
}
sec=0
timeout=30

# make sure the emulator is there - and in the PATH
echo y | sdkmanager "emulator"
avdmanager list avd
emulator -version
find /opt -iname emulator -type f

waitForAdbDevices() {
	while true; do
        	if [[ $sec -ge $timeout ]]; then
                	err "Timeout ($timeout seconds) reached - adb devices didn't find all emulators"
        	fi
		out=$(adb devices | grep -v List | awk '$2{print $1}' | wc -l)
       		if [[ "$out" == "$N" ]]; then
                	break
        	fi
        	let "r = sec % 50"
        	if [[ $r -eq 0 ]]; then
                	echo "Waiting for adb devices to start: $out / $N"
        	fi
        	sleep 1
        	let "sec++"
	done
}

#start first N avd images
Xvfb :0 -screen 0 800x600x16 &
#avdmanager list avd | grep 'Name:' | cut -d ':' -f2 | head -n $N |  xargs -I{} -P$N -n1 emulator -no-snapshot -avd {} &
avdmanager list avd | grep 'Name:' | cut -d ':' -f2 | head -n $N |  xargs -I{} -P$N -n1 emulator -no-window -no-audio -no-snapshot -avd {} &
#avdmanager list avd | grep 'Name:' | cut -d ':' -f2 | head -n $N |  xargs -I{} -P$N -n1 emulator -no-snapshot -no-window -avd {} &
# avdmanager list avd | grep 'Name:' | cut -d ':' -f2 | head -n $N |  xargs -I{} -P$N -n1 emulator -no-snapshot -no-window -no-boot-anim -accel on  -avd {} &
waitForAdbDevices
echo "adb found all emulators..."

#wait for each emulator that booting completed
adb devices | grep -v List | awk '$2{print $1}' | xargs -I{} .gitlab/wait-for-emulator.sh -s {}
echo "all emulators successfully booted"
