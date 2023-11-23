FROM registry.0xacab.org/leap/bitmask_android/android-sdk:latest

MAINTAINER LEAP Encryption Access Project <info@leap.se>
LABEL Description="Android SDK baseimage based on debian:bullseye" Vendor="LEAP" Version="34"

# Make sure debconf doesn't complain about lack of interactivity
ENV DEBIAN_FRONTEND noninteractive
# ensure GL compatibility
ENV ANDROID_EMULATOR_USE_SYSTEM_LIBS=1

# ------------------------------------------------------
# --- System Dependencies

# Need docker package in order to do Docker-in-Docker (DIND)
RUN apt-get update -qq && \
    apt-get -y dist-upgrade && \
    apt-get -y install gnupg apt-transport-https

RUN apt-get update -qq && \
    apt-get install -y docker-ce docker-ce-cli mesa-utils && \
    apt-get clean && \
    apt-get autoclean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*


# ------------------------------------------------------
# --- Install Android Emulator

# Install Android SDK emulator package
RUN echo y | sdkmanager "emulator"

# Install System Images for emulators4;google_apis;x86_64"
RUN echo y | sdkmanager "system-images;android-34;google_apis;x86_64"

RUN echo no | avdmanager create avd --force --name testApi34 --abi google_apis/x86_64 --package 'system-images;android-34;google_apis;x86_64'
