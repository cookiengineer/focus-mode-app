# Android build container for the Focus Mode app.
#
# Builds a headless CLI toolchain (JDK 17 + Android SDK + Gradle) that matches
# the AGP/Gradle/Kotlin versions used by the project. Intended to be used with
# rootless Podman; the project is mounted at /workspace.
#
#   podman build -t focus-mode-builder -f Containerfile .
#   scripts/build.sh assembleDebug

FROM debian:bookworm-slim

ARG ANDROID_SDK_ROOT=/opt/android-sdk
ARG ANDROID_CMDLINE_TOOLS_VERSION=11076708
ARG GRADLE_VERSION=8.11.1

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT} \
    ANDROID_HOME=${ANDROID_SDK_ROOT} \
    PATH=/opt/gradle/bin:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        git \
        openjdk-17-jdk-headless \
        unzip \
        wget \
 && rm -rf /var/lib/apt/lists/*

# --- Android SDK command line tools -----------------------------------------
RUN mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools" /opt/gradle \
 && wget -q -O /tmp/cmdline-tools.zip \
        "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS_VERSION}_latest.zip" \
 && unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools \
 && mv /tmp/cmdline-tools/cmdline-tools "${ANDROID_SDK_ROOT}/cmdline-tools/latest" \
 && rm -rf /tmp/cmdline-tools /tmp/cmdline-tools.zip

# Accept the SDK licenses and install the platforms/build-tools we compile against.
RUN yes | sdkmanager --sdk_root="${ANDROID_SDK_ROOT}" --licenses >/dev/null \
 && sdkmanager --sdk_root="${ANDROID_SDK_ROOT}" \
        "platform-tools" \
        "platforms;android-35" \
        "platforms;android-36" \
        "build-tools;35.0.0" \
        "build-tools;36.0.0" \
 && yes | sdkmanager --sdk_root="${ANDROID_SDK_ROOT}" --licenses >/dev/null

# --- Gradle -----------------------------------------------------------------
RUN wget -q -O /tmp/gradle.zip \
        "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
 && unzip -q /tmp/gradle.zip -d /opt \
 && rm -rf /opt/gradle \
 && mv "/opt/gradle-${GRADLE_VERSION}" /opt/gradle \
 && rm -f /tmp/gradle.zip

WORKDIR /workspace

# Keep the daemon out of the way for reproducible one-shot CLI builds.
CMD ["gradle", "--no-daemon", "assembleDebug"]
