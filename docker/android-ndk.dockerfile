FROM 0xacab.org:4567/leap/bitmask_android/android-sdk:latest

MAINTAINER LEAP Encryption Access Project <info@leap.se>
LABEL Description="Android NDK image based on android-sdk baseimage" Vendor="LEAP" Version="r12b"

# ------------------------------------------------------
# --- Install System Dependencies

RUN apt-get update -qq
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y \
    make gcc file lib32stdc++6 lib32z1 # JNI build dependencies w/ 32-bit compatible C libs

# ------------------------------------------------------
# --- Install Android NDK (for running C code)

# NOTE(@aguestuser|4.23.17)
# We woud like to use te current version of Android NDK (r14b) but cannot 
#   due to pinned dependency on year-old version of `ics-openvpn`
#   which has transitive dependency on `openssl` which will not compile with `clang`
#   (starting in 13b, android ndk uses `clang` isntead of `gcc`)
# Upon rebasing onto to current HEAD of `ics-openvpn` and resolving conflicts, we
#   should update to current version of `ndk`.

ENV ANDROID_NDK_VERSION "r12b"
ENV ANDROID_NDK_HOME ${ANDROID_HOME}/android-ndk-${ANDROID_NDK_VERSION}
ENV ANDROID_NDK_URL http://dl.google.com/android/repository/android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip

RUN curl -L $ANDROID_NDK_URL -o ndk.zip  \
    && unzip ndk.zip -d $ANDROID_HOME  \
    && rm -rf ndk.zip

ENV PATH ${PATH}:${ANDROID_NDK_HOME}
