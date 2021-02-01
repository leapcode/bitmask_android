#!/bin/bash

SCRIPT_DIR=$(dirname "$0")
BASE_DIR="$SCRIPT_DIR/.."

git checkout -- \*
git checkout -- \.\*

rm -r $BASE_DIR/ics-openvpn
rm -r $BASE_DIR/build
rm -r $BASE_DIR/app/build
rm -r $BASE_DIR/go/lib/*
rm -r $BASE_DIR/currentReleases
git submodule sync --recursive
git submodule update --init --recursive
