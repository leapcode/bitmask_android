FROM debian:stretch

MAINTAINER LEAP Encryption Access Project <info@leap.se>
LABEL Description="Android SDK baseimage based on debian:stretch" Vendor="LEAP" Version="25.2.5"

# ------------------------------------------------------
# --- Install System Dependencies

RUN apt-get update -qq
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y \
  # the basics
  curl unzip git locales \
  # java stuff
  openjdk-8-jdk maven

# ------------------------------------------------------
# --- Set Locales

# Generate All Locales
RUN cp /usr/share/i18n/SUPPORTED /etc/locale.gen
RUN locale-gen

# Set Default Locale
RUN localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
ENV LANG en_US.UTF-8

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
# --- Install Android SDK Tools Packages

# Install Platform Tools Package
RUN echo y | sdkmanager "platform-tools" # echo y to accept google licenses

# Install Android Support Repositories
RUN sdkmanager "extras;android;m2repository"

# Install Build Tools (Please keep in descending order)
RUN sdkmanager "build-tools;25.0.2"
RUN sdkmanager "build-tools;25.0.0"
RUN sdkmanager "build-tools;24.0.3"
RUN sdkmanager "build-tools;23.0.3"

# Install Target SDK Packages (Please keep in descending order)
RUN sdkmanager "platforms;android-25"
RUN sdkmanager "platforms;android-24"
RUN sdkmanager "platforms;android-23"

# ------------------------------------------------------
# --- Cleanup

RUN apt clean
