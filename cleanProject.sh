#!/bin/bash

git checkout -- \*
git checkout -- \.\*

rm -r ./ics-openvpn
rm -r ./build
rm -r ./app/build
rm -r ./go/lib/*
git submodule sync --recursive
git submodule update --init --recursive
