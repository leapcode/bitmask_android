# Bitmask Android App 

This repository contains the source code for the [Bitmask][https://bitmask.net/] Android app.

Please see the [issues](https://github.com/leapcode/bitmask_android/issues) section to
report any bugs or feature requests and to see the list of known issues.

## License

* [See LICENSE file](https://github.com/leapcode/bitmask_android/blob/master/LICENSE.txt)

## Building

The build requires [Ant](https://ant.apache.org/) v1.6+, the [Android SDK](http://developer.android.com/sdk/index.html) API 17 and the [Android NDK](http://developer.android.com/tools/sdk/ndk/index.html) r8b
to be installed in your development environment. 

In addition you'll need ant/bin, android/tools, 'platforms-tools' and 'android-ndk-r8b' in your enviroment path.

After satisfying those requirements, the build is pretty simple:

* Run `./compile.sh` from the project directory to build the APK only

You might find that your device doesn't let you install your build if you
already have the version from the Android Market installed.  This is standard
Android security as it it won't let you directly replace an app that's been
signed with a different key.  Manually uninstall Bitmask Android from your device and
you will then be able to install your own built version.
To uninstall it, do: adb uninstall se.leap.bitmaskclient

See [here](https://github.com/leapcode/bitmask_android/blob/master/Building_from_eclipse.md) for
instructions on building from [Eclipse](http://eclipse.org).

## Acknowledgements

This project uses code from [ics-openvpn project](https://code.google.com/p/ics-openvpn/).

## Contributing

Please fork this repository and contribute back using
[pull requests](https://github.com/leapcode/leap_android/pulls).

Any contributions, large or small, major features, bug fixes, additional
language translations, unit/integration tests are welcomed and appreciated
but will be thoroughly reviewed and discussed.
