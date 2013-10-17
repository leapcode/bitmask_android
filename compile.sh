#!/bin/bash
android update project --path . --name "Bitmask for Android" --target android-15
ant debug
