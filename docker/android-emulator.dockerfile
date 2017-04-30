FROM 0xacab.org:4567/leap/bitmask_android/android-sdk:latest

MAINTAINER LEAP Encryption Access Project <info@leap.se>
LABEL Description="Android SDK baseimage based on debian:stretch" Vendor="LEAP" Version="25"

# ------------------------------------------------------
# --- System Dependencies

# ensure GL compatibility

RUN apt-get update -qq
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y mesa-utils 
ENV ANDROID_EMULATOR_USE_SYSTEM_LIBS=1 

# ------------------------------------------------------
# --- Install Android Emulator

# Install Android SDK emulator package
RUN echo y | sdkmanager "emulator"

# Install System Images for emulators
RUN echo y | sdkmanager "system-images;android-25;google_apis;x86_64"
RUN echo y | sdkmanager "system-images;android-24;google_apis;x86_64"
RUN echo y | sdkmanager "system-images;android-23;google_apis;x86_64"
