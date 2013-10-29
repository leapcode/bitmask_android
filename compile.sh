#!/bin/bash
libopenvpn_so_files=`find libs -name libopenvpn.so | wc --lines`
libopvnutil_so_files=`find libs -name libopvpnutil.so | wc --lines`
minivpn_files=`find libs -name minivpn | wc --lines`
if [ $libopenvpn_so_files -lt 4 ] || [ $libopvnutil_so_files -lt 4 ] || [ $minivpn_files -lt 4 ];
then
	./compile-native-openvpn.sh
fi

android update project --path . --name "Bitmask for Android" --target android-15
ant debug
