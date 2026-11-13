# Battery Cycles

Battery cycle estimates and battery-health readings for supported Samsung Galaxy devices.

## Download
Get the signed APK from [Releases](https://github.com/dimitris-vlch/battery-cycles/releases). Android 11 or newer, ARM64.

## Setup
Connect your phone to Wi-Fi, enable Developer options and Wireless debugging, then follow the app pairing instructions. Pull down the notifications, tap the arrow on the Battery Cycles notification, tap "Code" and type the six-digit code. No computer is required. Wireless debugging must be available for fresh readings; cached readings are labelled with their timestamp.

## Limitations
The Samsung usage counter divided by 100 is an estimate, not a manufacturer-certified cycle count. Tested readings came from an S23 Ultra. Other firmware may not expose the fields. End-to-end wireless pairing without a computer was verified on 2026-11-13 on a Galaxy S23 Ultra (SM-S918B, Android 17), from a clean install through pairing to a cycle reading. It has not been tested on other models; report problems through Issues.

## Build
Install JDK 17 and Android SDK platform 36. Set ANDROID_HOME, then run `./gradlew assembleDebug` or `./gradlew assembleRelease`. Release builds are unsigned without a private keystore.properties file. Signing keys are never included in this repository.

## Privacy
Battery readings and pairing credentials are stored on the device. No advertising or analytics. See [privacy policy](https://battery-cycles-app.dimitris-vlachos-mbg.workers.dev/privacy).

## License
Project code: Apache-2.0. Dependencies retain their own licenses; see THIRD_PARTY_NOTICES.md.
