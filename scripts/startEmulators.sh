#!/bin/bash

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
waitForAdbDevices() {
	while true; do
        	if [[ $sec -ge $timeout ]]; then
                	err "Timeout ($timeout seconds) reached - adb devices didn't find all emulators"
        	fi
		out=$(adb devices | grep -v List | awk '$2{print $1}' | wc -l)
       		if [[ "$out" == "$N" ]]; then
                	break
        	fi
        	let "r = sec % 5"
        	if [[ $r -eq 0 ]]; then
                	echo "Waiting for adb devices to start: $out / $N"
        	fi
        	sleep 1
        	let "sec++"
	done
}

#start first N avd images
avdmanager list avd | grep Name: | cut -d ':' -f2 | head -n $N |  xargs -I{} -P$N -n1 emulator -no-snapshot -avd {} &
waitForAdbDevices
echo "adb found all emulators..."

#wait for each emulator that booting completed
adb devices | grep -v List | awk '$2{print $1}' | xargs -I{} .gitlab/wait-for-emulator.sh -s {}
echo "all emulators successfully booted"


