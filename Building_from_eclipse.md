# Download Eclipse ADT environment

# Download Android SDK for Bitmask Android

- From the Android SDK Manager, install API 17 (Android 4.2.2) SDK Platform and ARM EABI v7a System Image
- Restart Eclipse

# Setup an AVD

- Leave default settings
- Set the name (api_17 for example)
- Device 4.0 WVGA
- API level 17
- SD card size = 100 MiB

# Import project
## Import repository from Git

File -> Import -> Git -> Projects from Git
Uri -> https://github.com/leapcode/bitmask_android.git -> leave develop and master checked -> initial branch = develop -> leave "Import existing projects" -> deselect leap_androidTest -> Finish

## Build OpenVPN

- From the project directory, execute "./compile-native-openvpn.sh"
