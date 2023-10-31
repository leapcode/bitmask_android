FROM 0xacab.org:4567/leap/docker/debian:bullseye_amd64

MAINTAINER LEAP Encryption Access Project <info@leap.se>
LABEL Description="Android SDK baseimage based on debian:bullseye" Vendor="LEAP" Version="33.0.0"

# ------------------------------------------------------
# --- Install System Dependencies

# Make sure debconf doesn't complain about lack of interactivity
ENV DEBIAN_FRONTEND noninteractive

# Need docker package in order to do Docker-in-Docker (DIND)
RUN apt-get update -qq && \
    apt-get -y dist-upgrade && \
    apt-get -y install gnupg apt-transport-https curl

RUN curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian \
    $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list

RUN apt-get update -qq && \
    apt-get install -y docker-ce docker-ce-cli mesa-utils \
    # the basics
    curl unzip git locales \
    # java stuff \
    openjdk-17-jdk maven && \
    apt-get clean && \
    apt-get autoclean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# ------------------------------------------------------
# --- Set Locales

# Generate only en_US Locales
RUN locale-gen en_US.UTF-8

# Set Default Locale
RUN localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
ENV LANG en_US.UTF-8

# ------------------------------------------------------
# --- Install Android SDK Tools

ENV ANDROID_HOME /opt/android-sdk-linux
ENV ANDROID_SDK_URL https://dl.google.com/android/repository/commandlinetools-linux-7583922_latest.zip
ENV ANDROID_SDK_ROOT ${ANDROID_HOME}
ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk-amd64

RUN echo $JAVA_HOME
RUN java --version

# Install SDK Tools
RUN curl -L $ANDROID_SDK_URL -o sdk-tools.zip  \
    && mkdir -p /opt/android-sdk-linux \
    && unzip -q sdk-tools.zip -d $ANDROID_HOME \
    && mv -v $ANDROID_HOME/cmdline-tools $ANDROID_HOME/latest \
    && echo $(ls -l $ANDROID_HOME) \
    && mkdir -v $ANDROID_HOME/cmdline-tools \
    && mv -v $ANDROID_HOME/latest $ANDROID_HOME/cmdline-tools/ \
    && rm -f sdk-tools.zip

# Update PATH
ENV PATH ${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/cmdline-tools:${ANDROID_HOME}/cmd-tools/bin:${ANDROID_HOME}/platform-tools
ENV ANDROID_SDK_ROOT ${ANDROID_HOME}/cmdline-tools/latest

# Debug location and path

RUN echo $PATH && \
    echo $(ls /opt/) && \
    echo $(ls /opt/android-sdk-linux)  && \
    echo $(ls /opt/android-sdk-linux/cmdline-tools/latest)

# ------------------------------------------------------
# --- Install Android SDK Tools Packages

# Install Platform Tools Package
RUN echo y | sdkmanager "platform-tools" # echo y to accept google licenses

# Install Android Support Repositories
RUN sdkmanager "extras;android;m2repository"

# Install Build Tools (Please keep in descending order)
RUN sdkmanager "build-tools;33.0.2"

# Install Target SDK Packages (Please keep in descending order)
RUN sdkmanager "platforms;android-33"

RUN echo "accept all licenses"
# Accept all licenses
RUN yes | sdkmanager --licenses
