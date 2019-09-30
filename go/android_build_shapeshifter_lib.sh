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

./golang/go/bin/go get golang.org/x/mobile/cmd/gomobile

gomobile init
gomobile bind -target=android -o ./lib/shapeshifter.aar se.leap.bitmaskclient/shapeshifter/
cp lib/shapeshifter* ../shapeshifter/.