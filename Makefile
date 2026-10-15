# Focus Mode - convenience wrapper around scripts/build.sh.
#
# The actual build runs inside the Podman container defined by Containerfile,
# so no host Android SDK is required. Every Gradle task is delegated to
# scripts/build.sh.
#
#   make                 # debug + release APKs
#   make test            # JVM unit tests
#   make help            # list all targets

IMAGE        ?= focus-mode-builder
BUILD        := scripts/build.sh
APK          ?= app/build/outputs/apk/release/app-release.apk
ADB          ?= adb
DEVICE_OWNER := engineer.cookie.focusmode/engineer.cookie.focusmode.admin.FocusDeviceAdminReceiver

.DEFAULT_GOAL := all
.PHONY: all build debug release test lint clean distclean image rebuild-image \
        shell install set-device-owner help

all: build

## Build both the debug and the release APK.
build: debug release

## Build the debug APK (app/build/outputs/apk/debug/app-debug.apk).
debug:
	$(BUILD) assembleDebug

## Build the signed release APK (app/build/outputs/apk/release/app-release.apk).
release:
	$(BUILD) assembleRelease

## Run the JVM unit tests (no device required).
test:
	$(BUILD) testDebugUnitTest

## Run Android lint.
lint:
	$(BUILD) lintDebug

## Remove Gradle build output inside the container.
clean:
	$(BUILD) clean

## Remove build output, caches and the builder image.
distclean:
	rm -rf app/build .gradle .gradle-container .kotlin .android
	-podman rmi $(IMAGE)
	-$(BUILD) clean

## Build the Podman builder image.
image:
	podman build -t $(IMAGE) -f Containerfile .

## Force-rebuild the Podman builder image.
rebuild-image:
	$(BUILD) --rebuild assembleDebug

## Open an interactive shell inside the builder container.
shell:
	podman run --rm -it \
		--userns=keep-id \
		-v "$(CURDIR)":/workspace:Z \
		-w /workspace \
		-e HOME=/tmp \
		-e GRADLE_USER_HOME=/workspace/.gradle-container \
		$(IMAGE) \
		bash

## Install the APK on the connected device (override with APK=path).
install:
	$(ADB) install -r $(APK)

## Make Focus Mode the device owner so it can freeze other apps.
set-device-owner:
	$(ADB) shell dpm set-device-owner $(DEVICE_OWNER)

## Show this help.
help:
	@echo "Focus Mode build targets:"
	@echo ""
	@echo "  make                 build debug + release APKs"
	@echo "  make debug           build the debug APK"
	@echo "  make release         build the signed release APK"
	@echo "  make test            run JVM unit tests"
	@echo "  make lint            run Android lint"
	@echo "  make clean           remove Gradle build output"
	@echo "  make distclean       remove build output, caches and image"
	@echo "  make image           build the Podman builder image"
	@echo "  make rebuild-image   force-rebuild the builder image"
	@echo "  make shell           open a shell in the builder container"
	@echo "  make install         adb install the release APK"
	@echo "  make set-device-owner set Focus Mode as device owner"
	@echo ""
	@echo "  APK=<path> make install    install a specific APK"
