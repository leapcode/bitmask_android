#!/bin/bash

## --- System Dependencies for fastlane
apt-get update -qq && \
apt-get -y dist-upgrade && \
apt-get -y install make build-essential ruby ruby-dev imagemagick xvfb libxcb1 libname-dev

gem install fastlane
fastlane frameit download_frames

## ------------------------------------------------------
## --- Android Emulator 

sdkmanager "platforms;android-30"
#
# Install Android SDK emulator package
echo y | sdkmanager "emulator"

echo y | sdkmanager "system-images;android-31;google_apis;x86_64"
#echo y | sdkmanager "system-images;android-30;google_apis;x86_64"
#echo y | sdkmanager "system-images;android-28;google_apis;x86_64"
#echo y | sdkmanager "system-images;android-27;google_apis;x86"
#echo y | sdkmanager "system-images;android-25;google_apis;x86_64"

echo no | avdmanager create avd --force --name testApi31 --abi google_apis/x86_64 --package 'system-images;android-31;google_apis;x86_64'
echo no | avdmanager create avd --force --name testApiduet --abi google_apis/x86_64 --package 'system-images;android-31;google_apis;x86_64'

#echo no | avdmanager create avd --force --name testApi31 --abi google_apis/x86_64 --package 'system-images;android-30;google_apis;x86_64'
#echo no | avdmanager create avd --force --name testApiduet --abi google_apis/x86_64 --package 'system-images;android-30;google_apis;x86_64'

#echo no | avdmanager create avd --force --name testApi28 --abi google_apis/x86_64 --package 'system-images;android-28;google_apis;x86_64'
#echo no | avdmanager create avd --force --name testApiduet --abi google_apis/x86_64 --package 'system-images;android-28;google_apis;x86_64'
 
#echo no | avdmanager create avd --force --name testApi27 --abi google_apis/x86 --package 'system-images;android-27;google_apis;x86'
#echo no | avdmanager create avd --force --name testApiduet --abi google_apis/x86 --package 'system-images;android-27;google_apis;x86'
 
#echo no | avdmanager create avd --force --name testApi25 --abi google_apis/x86_64 --package 'system-images;android-25;google_apis;x86_64'
#echo no | avdmanager create avd --force --name testApiduet --abi google_apis/x86_64 --package 'system-images;android-25;google_apis;x86_64'

##bundle exec fastlane android bitmask_screenshots
