#!/bin/bash

function quit {
    echo "Task failed. Exit value: $?."
    exit 1
}


DIR_OVPNASSETS=./ics-openvpn/main/build/ovpnassets
DIR_OVPNLIBS=./ics-openvpn/main/build/intermediates/cmake/noovpn3/release/obj
# init
# look for empty dir 

if [[ $(ls -A ${DIR_OVPNASSETS}) && $(ls -A ${DIR_OVPNLIBS}) ]]
then
    echo "Dirty build: skipped externalNativeBuild - reusing existing libs"
else
    echo "Clean build: starting externalNativeBuild"
    cd ./ics-openvpn || quit
    ./gradlew clean main:externalNativeBuildCleanNoovpn3Release main:externalNativeBuildNoovpn3Release || quit
    cd .. 
fi