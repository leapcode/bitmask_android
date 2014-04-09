# Bitmask Android App 

This repository contains the source code for the [Bitmask](https://bitmask.net/) Android app.

Please see the [issues](https://github.com/leapcode/bitmask_android/issues) section to
report any bugs or feature requests and to see the list of known issues.

## License

* [See LICENSE file](https://github.com/leapcode/bitmask_android/blob/master/LICENSE.txt)

## Building

The build requires the [Android SDK](http://developer.android.com/sdk/index.html) API 17 and the [Android NDK](http://developer.android.com/tools/sdk/ndk/index.html) r9d
to be installed in your development environment.

In addition you'll need ant/bin, android/tools, 'platforms-tools' and 'android-ndk-r9d' in your enviroment path.

### Native sources

To build NDK sources, you need to issue these commands:

* cd app
* ./build-native.sh
* cd .. (to get back to the project directory)

### Command line

* ./gradlew build

### Android Studio

* Import project => select bitmask_android top folder

## Acknowledgements

This project bases its work in [ics-openvpn project](https://code.google.com/p/ics-openvpn/).

## Contributing

Please fork this repository and contribute back using
[pull requests](https://github.com/leapcode/leap_android/pulls).

Our preferred method for receiving translations is our [Transifex project](https://www.transifex.com/projects/p/bitmask-android).

Any contributions, large or small, major features, bug fixes, additional
language translations, unit/integration tests are welcomed and appreciated
but will be thoroughly reviewed and discussed.
