# Focus Mode

An Android app for LineageOS / AOSP **without Google Play services** that silences
all calls and notifications, except from an allowlist of emergency contacts.

[Screenshot](screenshot.png)

## Features

- Pick contacts from the system Contacts app to add them to the Allowlist.
- Toggle **Focus Mode**: enables Do Not Disturb priority mode, allowing only
  calls/messages from allowlisted (starred) contacts.
- Freezes (suspends) all other user-facing apps while Focus Mode is active, so
  they cannot run or notify you. Their icons stay on the launcher but are greyed
  out and cannot be opened. This covers third-party apps as well as preinstalled
  apps (Music, Gallery, Browser, ...); core system components (launcher,
  SystemUI, Settings, Contacts, the file picker, the default dialer/SMS, the
  keyboard) are never frozen.
- Keeps a configurable list of apps running (never frozen), e.g. a firewall or
  browser. NetGuard (`eu.faircode.netguard`) and Fennec (`org.mozilla.fennec_fdroid`)
  are kept by default; manage the list under "Allowed Apps".
- Keeps media playback and alarms audible while Focus Mode is on - only
  notifications and other apps are silenced.
- Rejects incoming calls from anyone not on the allowlist (Call Screening role).
- Runs as a foreground service with a silent, persistent status notification.
- No network permissions, no Google services.

## Freezing other apps (Device Owner Mode)

Freezing uses `DevicePolicyManager.setPackagesSuspended()` and therefore requires
Focus Mode to be the **device owner**. This is a one-time, root-free step done
from a computer via ADB:

```sh
adb shell dpm set-device-owner engineer.cookie.focusmode/engineer.cookie.focusmode.admin.FocusDeviceAdminReceiver;
```

Requirements and notes:

The device must have **no accounts** (no Google, Nextcloud/DAVx5, work profile,
secondary users, etc.) when the command is run, otherwise Android refuses it.

To reverse it, use **Deactivate** in the app (calls `clearDeviceOwnerApp`). A
plain uninstall is not enough while Focus Mode is the device owner. If you
built with `android:testOnly="true"`, you can also run
`adb shell dpm remove-active-admin engineer.cookie.focusmode/.admin.FocusDeviceAdminReceiver`.

Without device owner, Focus Mode still applies Do Not Disturb but does not freeze
apps, and shows a warning in the app.

## Requirements

- Android 12 or later (API 31+)
- Made for LineageOS without GApps.
- One-time grants: Contacts + Notifications, Do Not Disturb access, and the
  Call Screening role.
- Device owner (one-time ADB step) for the app-freezing feature. See
  [Setup](#setup).

## Setup

1. Install the APK (see [Building](#building)):

   ```sh
   adb install -r app/build/outputs/apk/debug/app-debug.apk;
   ```

2. Open **Focus Mode** and grant the in-app setup prompts: Contacts +
   Notifications, Do Not Disturb access, and the Call Screening role.

3. Make Focus Mode the device owner so it can freeze other apps. This is a
   one-time, root-free step done from a computer via ADB:

   ```sh
   adb shell dpm set-device-owner engineer.cookie.focusmode/engineer.cookie.focusmode.admin.FocusDeviceAdminReceiver
   ```

   The device must have **no accounts** (no Google, Nextcloud/DAVx5, work
   profile, secondary users, etc.) when you run this, otherwise Android refuses
   it. Reopen the app afterwards; the setup row turns green.

See [Freezing other apps (device owner)](#freezing-other-apps-device-owner) for
what the device owner can do and how to reverse it.

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

