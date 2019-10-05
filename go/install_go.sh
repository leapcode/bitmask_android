#!/bin/bash

GO_VERSION=go1.12.7.linux-amd64

if [[ $(ls -A ${GO_VERSION}.tar.gz) ]]
then
    echo "reusing downloaded golang bundle"
else
    echo "installing go lang bundle ${GO_VERSION}.tar.gz"
    curl -o $GO_VERSION.tar.gz https://dl.google.com/go/$GO_VERSION.tar.gz
fi

if [[ $(ls -A ./golang/*) ]]
then
    rm -r ./golang/*
fi
tar -C ./golang -xzf $GO_VERSION.tar.gz
