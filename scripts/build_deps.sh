#!/bin/bash

function quit {
    echo "Task failed. $1."
    exit 1
}

SCRIPT_DIR=$(dirname "$0")
BASE_DIR="$SCRIPT_DIR/.."
DIR_OVPNASSETS=./ics-openvpn/main/build/ovpnassets
DIR_OVPNLIBS=./ics-openvpn/main/build/intermediates/cmake/noovpn3/release/obj
DIR_GOLIBS=./go/lib/
FILE_X86=./go/out/x86/piedispatcherlib
FILE_ARM=./go/out/armeabi-v7a/piedispatcherlib

# init
# look for empty dir

cd $BASE_DIR
if [[ $(ls -A ${DIR_OVPNASSETS}) && $(ls -A ${DIR_OVPNLIBS}) ]]
then
    echo "Dirty build: skipped externalNativeBuild - reusing existing libs"
else
    echo "Clean build: starting externalNativeBuild"
    cd ./ics-openvpn || quit "Directory ics-opevpn not found"
    ./gradlew clean main:externalNativeBuildCleanSkeletonRelease main:externalNativeBuildSkeletonRelease --debug --stacktrace || quit "Build ics-openvpn native libraries failed"
    cd ..
fi

if [[ $(ls -A ${DIR_GOLIBS}) ]]
then
    echo "Dirty build: Reusing go libraries"
else
    echo "Clean build: compiling Go libraries"
    cd ./go || quit "Directory go not found"
    ./install_go.sh || quit "install_go.sh failed"
    ./android_build_web_core.sh || quit "android_build_web_core.sh (shapeshifter + pgpverify) failed"
    ./android_build_core.sh || quit "android build core (shapeshifter) failed"
    cd ..
fi
