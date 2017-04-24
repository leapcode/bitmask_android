FROM debian:stretch

MAINTAINER LEAP Encryption Access Project <info@leap.se>
LABEL Description="Android SDK baseimage based on debian:stretch" Vendor="LEAP" Version="0.0.1"

# ------------------------------------------------------
# --- Install System Dependencies

# Update Debian
RUN apt-get update -qq

# Install Debian Packages
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y \
  # the basics
  curl unzip git locales \
  # java stuff
  openjdk-8-jdk maven \ 
  # ndk dependencies
  make gcc file lib32stdc++6 lib32z1 # (incl. 32-bit compatible versions)

# Set Locale
RUN locale-gen en_US.UTF-8  
RUN localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8

# ------------------------------------------------------
# --- Install Android SDK Tools

ENV ANDROID_SDK_VERSION "25.2.5"
ENV ANDROID_HOME /opt/android-sdk-linux
ENV ANDROID_SDK_URL https://dl.google.com/android/repository/tools_r${ANDROID_SDK_VERSION}-linux.zip

# Install SDK Tools
RUN curl -L $ANDROID_SDK_URL -o sdk-tools.zip  \
    && unzip -q sdk-tools.zip -d $ANDROID_HOME \
    && rm -f sdk-tools.zip

# Update PATH
ENV PATH ${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools

# ------------------------------------------------------
# --- Install Android NDK (for running C code)

# NOTE(@aguestuser|4.23.17)
# We woud like to use te current version of Android NDK (r14b) but cannot 
#   due to pinned dependency on year-old version of `ics-openvpn`
#   which has transitive dependency on `openssl` which will not compile with `clang`
#   (starting in 13b, android ndk uses `clang` isntead of `gcc`)
# Upon rebasing onto to current HEAD of `ics-openvpn` and resolving conflicts, we
#   should update to current version of `ndk` (if possible).

ENV ANDROID_NDK_VERSION "r12b"
ENV ANDROID_NDK_HOME ${ANDROID_HOME}/android-ndk-${ANDROID_NDK_VERSION}
ENV ANDROID_NDK_URL http://dl.google.com/android/repository/android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip

RUN curl -L $ANDROID_NDK_URL -o ndk.zip  \
    && unzip ndk.zip -d $ANDROID_HOME  \
    && rm -rf ndk.zip

ENV PATH ${PATH}:${ANDROID_NDK_HOME}

# ------------------------------------------------------
# --- Install Android SDK Tools Packages


# Install Platform Tools Package
RUN echo y | sdkmanager "platform-tools" # echo y to accept google licenses

# Install Android Support Repositories
RUN echo y | sdkmanager "extras;android;m2repository"

# Install Target SDK Packages (Please keep in descending order)
RUN echo y | sdkmanager "platforms;android-25"
RUN echo y | sdkmanager "platforms;android-24"
RUN echo y | sdkmanager "platforms;android-23"

# Install Build Tools (Please keep in descending order)
RUN echo y | sdkmanager "build-tools;25.0.2"
RUN echo y | sdkmanager "build-tools;25.0.0"
RUN echo y | sdkmanager "build-tools;24.0.3"
RUN echo y | sdkmanager "build-tools;23.0.3"

# ------------------------------------------------------
# --- Install Android Emulator


# RUN echo y | sdkmanager "emulator"

# System Images for emulators
# RUN echo y | sdkmanager "system-images;android-25;google_apis;armeabi-v7a"
# RUN echo y | sdkmanager "system-images;android-24;google_apis;armeabi-v7a"
# RUN echo y | sdkmanager "system-images;android-23;google_apis;armeabi-v7a"
# RUN echo y | sdkmanager "system-images;android-23;google_apis;arm64-v8a"

# ------------------------------------------------------
# --- Cleanup

RUN apt-get clean

