#!/bin/bash
function compile() {
    svn co http://google-breakpad.googlecode.com/svn/trunk/ google-breakpad
    ./build-native.sh
}

if command -v $(head -n 1 build-native.sh | column | cut -d ' ' -f 1); then
	compile
elif command -v ndk-build; then
	sed -i 's/.*ndk-build/ndk-build/g' build-native.sh
	compile
else
	echo "Install ndk, or modify build-native script to point to your ndk-build executable"
fi
