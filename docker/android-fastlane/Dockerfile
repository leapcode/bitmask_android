FROM registry.0xacab.org/leap/bitmask_android/android-ndk:latest

MAINTAINER LEAP Encryption Access Project <info@leap.se>
LABEL Description="Android emulator image based on android-sdk" Vendor="LEAP" Version="4"

# Make sure debconf doesn't complain about lack of interactivity
ENV DEBIAN_FRONTEND noninteractive
# ensure GL compatibility
ENV ANDROID_EMULATOR_USE_SYSTEM_LIBS=1

# ------------------------------------------------------
# --- System Dependencies

# Need docker package in order to do Docker-in-Docker (DIND)
RUN lsb_release -a

RUN apt-get update -qq && \
    apt-get -y dist-upgrade && \
    apt-get -y install gnupg apt-transport-https

# Docker apt details should be inherited from android_sdk
#RUN curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
#    echo \
#      "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian \
#      $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# JNI build dependencies w/ 32-bit compatible C libs
# fastlane dependencies

RUN apt-get update -qq && \
    apt-get install -y docker-ce docker-ce-cli make gcc swig file lib32stdc++6 lib32z1 \
    autoconf autogen automake autopoint autotools-dev gettext-base libtool patch pkg-config po4a \
    curl git openjdk-17-jdk openjdk-17-jre-headless imagemagick libpulse0 po4a \
    make build-essential ruby-dev imagemagick docker-ce-cli mesa-utils xvfb \
    libstdc++6 libncurses5 libsdl1.2debian imagemagick libpulse-java libpulse0 libxkbcommon-x11-0 && \
    apt-get clean && \
    apt-get autoclean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN echo $JAVA_HOME
RUN java --version


# ------------------------------------------------------
# --- Install Android Emulator

RUN echo "accept all licenses"
# Accept all licenses
RUN echo y | sdkmanager --licenses
RUN sdkmanager --list

# Install Android SDK emulator package
RUN echo y | sdkmanager "emulator"

# Install System Images for emulators
# seems like every version has its own quirks
RUN echo y | sdkmanager "system-images;android-34;google_apis;x86_64"

# fastlane wants 2 emulators. Starting happens in the scripts
RUN echo no | avdmanager create avd --force --name testApi34 --abi google_apis/x86_64 --package 'system-images;android-34;google_apis;x86_64'
# TODO: fastlane has been troublesome for newer versions, need to dive deep to update

# Install Android cmake
RUN sdkmanager "cmake;3.10.2.4988404"

# Fastlane:
RUN gem install bundler fastlane


###############################################

# this stuff is all just notes... really, don't call it a mess
# ------------------------------------------------------
# --- Install Android NDK (for running C code)

#ENV ANDROID_NDK_VERSION "r21e"
#ENV ANDROID_NDK_HOME ${ANDROID_HOME}/android-ndk-${ANDROID_NDK_VERSION}
#ENV ANDROID_NDK_URL http://dl.google.com/android/repository/android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip
#ENV ANDROID_SDK_ROOT ${ANDROID_HOME}/latest/cmdline-tools
#
#RUN curl -L $ANDROID_NDK_URL -o ndk.zip  \
#    && unzip ndk.zip -d $ANDROID_HOME/ndk  \
#    && rm -rf ndk.zip
#RUN cat $ANDROID_HOME/ndk/android-ndk-${ANDROID_NDK_VERSION}/source.properties | \
#    grep Pkg.Revision | cut -d "=" -f 2 | \
#    xargs -I '{}' mv $ANDROID_HOME/ndk/android-ndk-${ANDROID_NDK_VERSION}/ $ANDROID_HOME/ndk/'{}'
#ENV PATH ${PATH}:${ANDROID_NDK_HOME}

