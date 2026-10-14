# Focus Mode

An Android app for LineageOS / AOSP **without Google Play services** that silences
all calls and notifications, except from an allowlist of emergency contacts.

## Features

- Pick contacts from the system Contacts app to add them to the Allowlist.
- Toggle **Focus Mode**: enables Do Not Disturb priority mode, allowing only
  calls/messages from allowlisted (starred) contacts.
- Rejects incoming calls from anyone not on the allowlist (Call Screening role).
- Runs as a foreground service with a silent, persistent status notification.
- No network permissions, no Google services.

## Requirements

- Android 12 or later (API 31+)
- Made for LineageOS without GApps.
- One-time grants: Contacts + Notifications, Do Not Disturb access, and the
  Call Screening role.

## Building

The App is built inside a Podman container. No host Android SDK required.

```sh
scripts/build.sh assembleDebug;          # debug APK
scripts/build.sh assembleRelease;        # signed release APK
scripts/build.sh --rebuild assembleDebug;
```

Release builds are signed using `keystore.properties` (see `keystore/` folder).
If that file is absent it falls back to the debug key.

The build script outputs the following APK files:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

Install on a device locally via ADB:

```sh
adb install -r app/build/outputs/apk/release/app-release.apk;
```

## License

This project is released under the [MIT License](LICENSE_MIT.txt).

