#!/bin/bash

function quit {
    echo "Task failed. $1."
    exit 1
}

SCRIPT_DIR=$(dirname "$0")
BASE_DIR="$SCRIPT_DIR/.."
DIR_OVPNASSETS=./ics-openvpn/main/build/ovpnassets
DIR_OVPNLIBS=./ics-openvpn/main/build/intermediates/cmake/skeletonRelease/obj
DIR_GOLIBS=./bitmaskcore/lib/
#FILE_X86=./go/out/x86/piedispatcherlib
#FILE_ARM=./go/out/armeabi-v7a/piedispatcherlib
DIR_TORLIBS=./tor-android/external/lib
EXPECTED_NDK_VERSION="21.4.7075529"
EXPECTED_ANDROID_NDK_RELEASE_VERSION="r21e"
if [[ -z $BUILD_TOR ]]; then BUILD_TOR=true; fi
if [[ -z $BUILD_OPENVPN_LIBS ]]; then BUILD_OPENVPN_LIBS=true; fi

# init
# look for empty dir

cd $BASE_DIR

# try to set the expected ndk version
if [[ $(ls -A ${ANDROID_HOME}/ndk/${EXPECTED_NDK_VERSION}) ]]
then
  ANDROID_NDK_HOME=${ANDROID_HOME}/ndk/${EXPECTED_NDK_VERSION}
elif [[ -f ${ANDROID_HOME/android-ndk-}${EXPECTED_ANDOID_NDK_RELEASE_VERSION }} ]]; then
  echo "make sure you have the right ndk version installed and paths are set correctly"
  exit 1
else
  # ndk was manually downloaded from http://dl.google.com/android/repository
  ANDROID_NDK_HOME=${ANDROID_HOME}/android-ndk-${EXPECTED_ANDROID_NDK_RELEASE_VERSION}
fi
NDK_VERSION=`cat ${ANDROID_NDK_HOME}/source.properties | grep Pkg.Revision | cut -d "=" -f2 | sed 's/ //g'`

ls -la ${ANDROID_HOME}/*/*ndk
echo "ndk version: $NDK_VERSION"
echo "ANDROID_NDK_HOME: $ANDROID_NDK_HOME"

# build tor libs
if [[ ${BUILD_TOR} == false ]]; then
  echo "skipping Tor"
elif [[ $(ls -A ${DIR_TORLIBS}) ]]; then
  echo "Dirty build: Reusing tor libraries"
else
  echo "Clean build: compiling tor libraries"
  if [[ ! -d $DIR_TORLIBS ]]
  then
    mkdir $DIR_TORLIBS
  fi
  cd ./tor-android
  if [[ $NDK_VERSION == $EXPECTED_NDK_VERSION ]]
  then
    ./tor-droid-make.sh fetch -c || quit "failed to checkout tor dependencies"
    ./tor-droid-make.sh build -b release || quit "failed to build tor release binaries"
    ./gradlew --stop
  else
    quit "expected NDK VERSION: $EXPECTED_NDK_VERSION. But found: $NDK_VERSION"
  fi
  cd ..
fi

# build openvpn libs
if [[ ${BUILD_OPENVPN_LIBS} == false ]]; then
  echo "skipping openvpn"
elif [[ $(ls -A ${DIR_OVPNASSETS}) && $(ls -A ${DIR_OVPNLIBS}) ]]; then
    echo "Dirty build: skipped externalNativeBuild - reusing existing libs"
else
    echo "Clean build: starting externalNativeBuild"
    cd ./ics-openvpn || quit "Directory ics-opevpn not found"
    ./gradlew clean main:externalNativeBuildCleanSkeletonOvpn2Release main:externalNativeBuildSkeletonOvpn2Release --debug --stacktrace || quit "Build ics-openvpn native libraries failed"
    ./gradlew --stop
    cd ..
fi

# build bitmask core (snowflake, pgpverify)
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
    cd ..
fi
