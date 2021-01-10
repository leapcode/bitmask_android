FROM 0xacab.org:4567/leap/bitmask_android/android-sdk:latest

MAINTAINER LEAP Encryption Access Project <info@leap.se>
LABEL Description="Android SDK baseimage based on debian:stretch" Vendor="LEAP" Version="26"

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
RUN echo 'deb https://apt.dockerproject.org/repo debian-stretch main'> /etc/apt/sources.list.d/docker.list && \
    curl -s https://apt.dockerproject.org/gpg | apt-key add -
RUN apt-get update -qq && \
    apt-get -y install docker-engine mesa-utils && \
    apt-get clean && \
    apt-get autoclean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*


# ------------------------------------------------------
# --- Install Android Emulator

# Install Android SDK emulator package
RUN echo y | sdkmanager "emulator"

# Install System Images for emulators
RUN echo y | sdkmanager "system-images;android-30;google_apis;x86"
# RUN echo y | sdkmanager "system-images;android-27;google_apis;x86"
# RUN echo y | sdkmanager "system-images;android-25;google_apis;x86_64"
# RUN echo y | sdkmanager "system-images;android-23;google_apis;x86_64"

RUN echo no | avdmanager create avd --force --name testApi30 --abi google_apis/x86 --package 'system-images;android-30;google_apis;x86'