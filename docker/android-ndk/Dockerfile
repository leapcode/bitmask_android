FROM registry.0xacab.org/leap/bitmask_android/android-sdk:latest

MAINTAINER LEAP Encryption Access Project <info@leap.se>
LABEL Description="Android NDK image based on android-sdk baseimage" Vendor="LEAP" Version="r21e"

# Make sure debconf doesn't complain about lack of interactivity
ENV DEBIAN_FRONTEND noninteractive

# ------------------------------------------------------
# --- Install System Dependencies
# Need docker package in order to do Docker-in-Docker (DIND)
RUN lsb_release -a

RUN apt-get update -qq && \
    apt-get -y dist-upgrade && \
    apt-get install -y gnupg apt-transport-https

# Docker apt details should be inherited from android_sdk
#RUN curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
#    echo \
#      "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian \
#      $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# JNI build dependencies w/ 32-bit compatible C libs
RUN apt-get update -qq && \
    apt-get -y install docker-ce docker-ce-cli make gcc swig file lib32stdc++6 lib32z1 \
    autoconf autogen automake autopoint autotools-dev gettext-base libtool patch pkg-config po4a && \
    apt-get clean && \
    apt-get autoclean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# ------------------------------------------------------
# --- Install Android NDK (for running C code)

ENV ANDROID_NDK_VERSION "r21e"
ENV ANDROID_NDK_HOME ${ANDROID_HOME}/android-ndk-${ANDROID_NDK_VERSION}
ENV ANDROID_NDK_URL http://dl.google.com/android/repository/android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip
ENV ANDROID_SDK_ROOT ${ANDROID_HOME}/latest/cmdline-tools

RUN curl -L $ANDROID_NDK_URL -o ndk.zip  \
    && unzip ndk.zip -d $ANDROID_HOME/ndk  \
    && rm -rf ndk.zip
RUN cat $ANDROID_HOME/ndk/android-ndk-${ANDROID_NDK_VERSION}/source.properties | \
    grep Pkg.Revision | cut -d "=" -f 2 | \
    xargs -I '{}' mv $ANDROID_HOME/ndk/android-ndk-${ANDROID_NDK_VERSION}/ $ANDROID_HOME/ndk/'{}'
ENV PATH ${PATH}:${ANDROID_NDK_HOME}

RUN echo "accept all licenses"
# Accept all licenses
RUN echo y | sdkmanager --licenses
RUN sdkmanager --list

# Install Android cmake
RUN sdkmanager "cmake;3.10.2.4988404"
