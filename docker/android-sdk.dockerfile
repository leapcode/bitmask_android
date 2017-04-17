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
  wget unzip git locales \
  # java stuff
  openjdk-8-jdk maven \ 
  # c libraries
  make clang lib32stdc++6 lib32z1 # (incl. 32-bit compatible versions)

# libgcc-6-dev-arm64-cross

# Set Locale
RUN locale-gen en_US.UTF-8  
RUN localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8

# ------------------------------------------------------
# --- Install Android SDK Tools

ENV ANDROID_HOME /opt/android-sdk-linux
ENV SDK_TOOLS_VERSION "25.2.5"

# Install SDK Tools
RUN cd /opt \
    && wget -q -O sdk-tools.zip \
    https://dl.google.com/android/repository/tools_r${SDK_TOOLS_VERSION}-linux.zip \
    && unzip -q sdk-tools.zip -d ${ANDROID_HOME} \
    && rm -f sdk-tools.zip

# Update PATH
ENV PATH ${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools

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
RUN echo y | sdkmanager "build-tools;24.0.3"
RUN echo y | sdkmanager "build-tools;23.0.3"

# ------------------------------------------------------
# --- Install Android NDK (for running C code)

ENV ANDROID_NDK_HOME ${ANDROID_HOME}/ndk-bundle

# Install NDK packages from sdk tools

RUN echo y | sdkmanager "ndk-bundle"
RUN echo y | sdkmanager "cmake;3.6.3155560"
RUN echo y | sdkmanager "lldb;2.3"

# Update PATH

ENV PATH ${PATH}:${ANDROID_NDK_HOME}


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

