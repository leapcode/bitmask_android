#!/bin/bash
android update project --path . --name "LEAP Android" --target android-16
ant debug
