#!/bin/bash
cd ./ics-openvpn
./gradlew clean main:externalNativeBuildCleanNoovpn3Release main:externalNativeBuildNoovpn3Release
cd ..
./gradlew clean assembleDebug --stacktrace
