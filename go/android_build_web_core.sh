#!/bin/bash

export GOPATH=`pwd`
export GO_LANG=`pwd`/golang/go/bin
export GO_COMPILED=`pwd`/bin
PATH="${GO_LANG}:${GO_COMPILED}:${PATH}"

if [ -z $ANDROID_NDK_HOME ]; then
        echo "Android NDK path not specified!"
        echo "Please set \$ANDROID_NDK_HOME before starting this script!"
        exit 1;
fi

./golang/go/bin/go env
echo "getting gomobile..."
./golang/go/bin/go get golang.org/x/mobile/cmd/gomobile
echo "initiating gomobile..."
./bin/gomobile init
if [ ! -d ./lib ]; then mkdir ./lib; fi
echo "cross compiling bitmask web apk core lib (shapeshifter, pgpverify)..."
./bin/gomobile bind -target=android -o ./lib/bitmask-web-core.aar se.leap.bitmaskclient/shapeshifter/ se.leap.bitmaskclient/pgpverify
cp lib/bitmask-web-core* ../bitmask-web-core/.