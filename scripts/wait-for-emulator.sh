#!/bin/bash

# from https://gitlab.com/fdroid/fdroidclient
# changes by cyberta

sec=0
timeout=360

for ((i=1;i<=$#;i++));
do
        if [[ ${!i} = "-s" ]]
        then
                ((i++))
                SERIAL=${!i}
        elif [[ ${!i} = "-h" ]]
        then
                echo -e "
                -s      serial identifier of the emulator
                -h      print help
                "
                exit
        fi
done

if [[ -z $SERIAL ]]
then
	DEFAULTEMULATOR=true
	echo "using default emulator" 
fi

err() {
	echo "$@"
	exit 1
}

explain() {
	if [[ "$1" =~ "not found" ]]; then
		printf "device not found"
	elif [[ "$1" =~ "offline" ]]; then
		printf "device offline"
	elif [[ "$1" =~ "running" ]]; then
		printf "booting"
	else
		printf "$1"
	fi
}


while true; do
	if [[ $sec -ge $timeout ]]; then
		err "Timeout ($timeout seconds) reached - Failed to start emulator"
	fi
	if [[ ! -z $SERIAL ]]
	then
		out=$(adb -s $SERIAL shell getprop init.svc.bootanim 2>&1 | grep -v '^\*')
	else
		out=$(adb -e shell getprop init.svc.bootanim 2>&1 | grep -v '^\*')
	fi
	if [[ "$out" =~ "command not found" ]]; then
		err "$out"
 	fi
	if [[ "$out" =~ "stopped" ]]; then
		break
	fi
	let "r = sec % 5"
	if [[ $r -eq 0 ]]; then
		echo "Waiting for emulator $SERIAL to start: $(explain "$out")"
	fi
	sleep 1
	let "sec++"
done

echo "Emulator $SERIAL is ready"
