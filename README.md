# Battery Cycles

Battery cycle estimates and battery-health readings for supported Samsung Galaxy devices.

## Download
Get the signed APK from [Releases](https://github.com/dimitris-vlch/battery-cycles/releases). Android 11 or newer, ARM64.

## Setup
Connect your phone to Wi-Fi, enable Developer options and Wireless debugging, then follow the app pairing instructions. Enter the six-digit code through the notification. A computer is not required by the intended setup flow. Wireless debugging must be available for fresh readings; cached readings are labelled with their timestamp.

## Limitations
The Samsung usage counter divided by 100 is an estimate, not a manufacturer-certified cycle count. Tested readings came from an S23 Ultra. Other firmware may not expose the fields. End-to-end wireless pairing has not yet been verified in this project's recorded testing; report problems through Issues. This release is experimental.

## Build
Install JDK 17 and Android SDK platform 36. Set ANDROID_HOME, then run `./gradlew assembleDebug` or `./gradlew assembleRelease`. Release builds are unsigned without a private keystore.properties file. Signing keys are never included in this repository.

## Privacy
Battery readings and pairing credentials are stored on the device. No advertising or analytics. See [privacy policy](https://battery-cycles-app.dimitris-vlachos-mbg.workers.dev/privacy).

## License
Project code: Apache-2.0. Dependencies retain their own licenses; see THIRD_PARTY_NOTICES.md.
