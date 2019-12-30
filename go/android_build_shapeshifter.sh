#!/bin/bash

# Copyright (C) 2016 Andrew Jiang (TunnelBear Inc.)
# Convenience script for generating shapeshifter-dispatcher binaries for Android devices
# adapted for Bitmask by cyberta

BUILD_LIBRARY=false;

function quit {
    echo "$1."
    exit 1
}

if [ "$1" == "removeAll" ]; then
        echo "removing golang, sources and generated files"
        for folder in /tmp/android-toolchain-*; do
                if [[ -d $folder ]]; then
                        rm -rf $folder
                fi
        done
        if [[ -d "./out" ]]; then
                rm -rf ./out
        fi

        if [[ -d "./bin" ]]; then
                rm -rf ./bin
        fi

        if [[ -d "./golang" ]]; then
                rm -rf ./golang
        fi

        if [[ -d "./src" ]]; then
                rm -rf ./src
        fi
        echo "Done!"

elif [ "$1" == "clean" ]; then
        echo "Cleaning up..."
        for folder in /tmp/android-toolchain-*; do
                if [[ -d $folder ]]; then
                        rm -rf $folder
                fi
        done
        if [[ -d "./out" ]]; then
                rm -rf ./out
        fi

        if [[ -d "./bin" ]]; then
                rm -rf ./bin
        fi
        echo "Done!"
else
        if [[ "$1" == "createLibrary" ]]; then
            BUILD_LIBRARY=true
        fi

        if [ -z $ANDROID_NDK_HOME ]; then
                echo "Android NDK path not specified!"
                echo "Please set \$ANDROID_NDK_HOME before starting this script!"
                exit 1;
        fi

        if [[ ! -f ./bin/gomobile && $BUILD_LIBRARY == true ]]; then
            echo "gomobile not installed"
            echo please run "install_go.sh first"
            exit 1
        fi

        # Our targets are x86, x86_64, armeabi, armeabi-v7a, armv8;
        # To remove targets, simply delete them from the bracket.
        # NOTE: We are only currently shipping the armeabi-v7a binary
        # on Android, for space reasons.
        targets=(386 x86_64 armv7 arm64)
        export GOOS=android

        for arch in ${targets[@]}; do
                # Initialize variables
                go_arch=$arch
                ndk_arch=$arch
                ndk_platform="android-16"
                suffix=$arch

                if [ "$arch" = "386" ]; then
                        export CGO_ENABLED=1
                        ndk_arch="x86"
                        suffix="x86"
                        binary="i686-linux-android-gcc"
                elif [ "$arch" = "x86_64" ]; then
                        ndk_platform="android-21"
                        ndk_arch="x86_64"
                        suffix="x86_64"
                        binary="x86_64-linux-android-gcc"
                elif [ "$arch" = "armv5" ]; then
                        export GOARM=5
                        export CGO_ENABLED=1
                        go_arch="arm"
                        ndk_arch="arm"
                        suffix="armeabi"
                        binary="arm-linux-androideabi-gcc"
                elif [ "$arch" = "armv7" ]; then
                        export GOARM=7
                        export CGO_ENABLED=1
                        go_arch="arm"
                        ndk_arch="arm"
                        suffix="armeabi-v7a"
                        binary="arm-linux-androideabi-gcc"
                elif [ "$arch" = "arm64" ]; then
                        suffix="arm64-v8a"
                        ndk_platform="android-21"
                        binary="aarch64-linux-android-gcc"
                elif [ "$arch" = "mips" ]; then
                        binary="mipsel-linux-android-gcc"
                fi
                export GOARCH=${go_arch}
                export GOPATH=`pwd`
                export NDK_TOOLCHAIN=/tmp/android-toolchain-${ndk_arch}

                # Only generate toolchain if it does not already exist
                if [ ! -d $NDK_TOOLCHAIN ]; then
                        echo "Generating ${suffix} toolchain..."
                        $ANDROID_NDK_HOME/build/tools/make-standalone-toolchain.sh \
                        --arch=${ndk_arch} --platform=${ndk_platform} --install-dir=$NDK_TOOLCHAIN || quit "Failed to generate toolchain"
                        echo "Toolchain generated!"
                fi

                export CC=$NDK_TOOLCHAIN/bin/clang
                echo "Starting compilation for $suffix..."

                if [[ BUILD_LIBRARY ]]; then
                echo "cross compiling shapeshifter lib..."
                ./bin/gomobile bind -target=android -o ./lib/shapeshifter.aar se.leap.bitmaskclient/shapeshifter/
                cp lib/shapeshifter* ../shapeshifter/.
                    #./android_build_shapeshifter_lib.sh || quit "Failed to cross-compile shapeshifter-dispatcher-library"

                else
                    ./golang/go/bin/go build -buildmode=pie -ldflags '-w -s -extldflags=-pie' -o ./out/${suffix}/piedispatcher github.com/OperatorFoundation/shapeshifter-dispatcher/shapeshifter-dispatcher || quit "Failed to cross-compile shapeshifter-dispatcher"
                fi
                echo "Build succeeded!"

        done
fi