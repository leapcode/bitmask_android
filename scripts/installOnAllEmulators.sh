#!/bin/bash
adb devices | grep emulator | cut --output-delimiter '   ' -f1 | xargs -I{} adb -s {} install -r $1
