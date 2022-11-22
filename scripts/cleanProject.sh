#!/bin/bash

SCRIPT_DIR=$(dirname "$0")
BASE_DIR="$SCRIPT_DIR/.."

git checkout -- \*
git checkout -- \.\*

git clean -xfd
git submodule foreach --recursive git clean -xfd
rm -r $BASE_DIR/ics-openvpn
rm -r $BASE_DIR/build
rm -r $BASE_DIR/app/build
rm -r $BASE_DIR/bitmaskcore/lib/*
rm -r $BASE_DIR/bitmaskcore/golang/
rm -r $BASE_DIR/currentReleases
rm -r $BASE_DIR/tor-android/build
rm -r $BASE_DIR/tor-android/tor-android-binary/build
rm -r $BASE_DIR/tor-android/external/bin
rm -r $BASE_DIR/tor-android/external/include/
rm -r $BASE_DIR/tor-android/external/*.build-stamp
rm -r $BASE_DIR/tor-android/external/lib
git reset --hard
git submodule foreach --recursive git reset --hard HEAD
git submodule sync --recursive
git submodule update --init --recursive
