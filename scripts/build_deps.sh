#!/bin/bash

function quit {
    echo "Task failed. $1."
    exit 1
}

SCRIPT_DIR=$(dirname "$0")
BASE_DIR="$SCRIPT_DIR/.."
DIR_OVPNASSETS=./ics-openvpn/main/build/ovpnassets
DIR_OVPNLIBS=./ics-openvpn/main/build/intermediates/cmake/noovpn3/release/obj
DIR_GOLIBS=./bitmaskcore/lib/
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
    cd ./bitmaskcore || quit "Directory go not found"
    if [[ ! -d lib ]]
    then
        mkdir lib
    fi
    ./build_core.sh || quit "failed to build bitmaskcore"
    cp lib/bitmaskcore.aar ../lib-bitmask-core/.
    cp lib/bitmaskcore-sources.jar ../lib-bitmask-core/.
    cp lib/bitmaskcore_web.aar ../lib-bitmask-core-web/.
    cp lib/bitmaskcore_web-sources.jar ../lib-bitmask-core-web/.
    cp lib/bitmaskcore_arm.aar ../lib-bitmask-core-armv7/.
    cp lib/bitmaskcore_arm-sources.jar ../lib-bitmask-core-armv7/.
    cp lib/bitmaskcore_arm64.aar ../lib-bitmask-core-arm64/.
    cp lib/bitmaskcore_arm64-sources.jar ../lib-bitmask-core-arm64/.
    cp lib/bitmaskcore_x86.aar ../lib-bitmask-core-x86/.
    cp lib/bitmaskcore_x86-sources.jar ../lib-bitmask-core-x86/.
    cp lib/bitmaskcore_x86_64.aar ../lib-bitmask-core-x86_64/.
    cp lib/bitmaskcore_x86_64-sources.jar ../lib-bitmask-core-x86_64/.

fi
