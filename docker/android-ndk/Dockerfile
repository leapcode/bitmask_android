FROM registry.0xacab.org/leap/bitmask_android/android-sdk:latest

LABEL Author="LEAP Encryption Access Project <info@leap.se>"
LABEL Description="Android NDK image based on android-sdk baseimage" Vendor="LEAP" Version="r28c"

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

ENV ANDROID_NDK_VERSION "r28c"
ENV ANDROID_NDK_VERSION_NUMBER "28.2.13676358"
ENV ANDROID_NDK_URL http://dl.google.com/android/repository/android-ndk-${ANDROID_NDK_VERSION}-linux.zip
ENV ANDROID_SDK_ROOT ${ANDROID_HOME}/latest/cmdline-tools

RUN echo $JAVA_HOME
RUN java --version


RUN curl -L $ANDROID_NDK_URL -o ndk.zip  \
    && unzip ndk.zip -d $ANDROID_HOME/ndk  \
    && rm -rf ndk.zip \
    && mv $ANDROID_HOME/ndk/android-ndk-${ANDROID_NDK_VERSION}/ $ANDROID_HOME/ndk/$ANDROID_NDK_VERSION_NUMBER

ENV ANDROID_NDK_HOME ${ANDROID_HOME}/ndk/$ANDROID_NDK_VERSION_NUMBER
ENV PATH ${PATH}:${ANDROID_NDK_HOME}

RUN echo "accept all licenses"
# Accept all licenses
RUN echo y | sdkmanager --licenses
RUN sdkmanager --list

# Install Android cmake
RUN sdkmanager "cmake;4.1.2"

# Install Go environment
ENV GO_VERSION=go1.24.12.linux-amd64
ENV EXPECTED_FP=bddf8e653c82429aea7aec2520774e79925d4bb929fe20e67ecc00dd5af44c50
ENV GO_DOWNLOAD_URL=https://golang.org/dl/go${GO_VERSION}.linux-amd64.tar.gz
ENV GOROOT=/usr/local/go
ENV GOBIN=/usr/local/go/bin
ENV PATH=$GOBIN:$PATH

RUN curl -L -o "go.tar.gz" "https://golang.org/dl/${GO_VERSION}.tar.gz"; \
    echo "${EXPECTED_FP} go.tar.gz" | sha256sum -c - && \
    tar -C /usr/local -xzf go.tar.gz && \
    rm go.tar.gz \


