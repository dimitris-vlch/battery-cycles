# Battery Cycles

Battery cycle estimates and battery-health readings for supported Samsung Galaxy devices.

## Download · Λήψη
**[⬇ Download Battery Cycles 1.1 (APK)](https://github.com/dimitris-vlch/battery-cycles/releases/download/v1.1/BatteryCycles-1.1.apk)** · Android 11+, ARM64, free.

Open the APK on your phone and allow installation from your browser if asked. All versions are under [Releases](https://github.com/dimitris-vlch/battery-cycles/releases).

**Ελληνικά:** πάτα τον σύνδεσμο από το κινητό, άνοιξε το APK και επίτρεψε την εγκατάσταση αν ζητηθεί. Η εφαρμογή δείχνει εκτίμηση για τους κύκλους φόρτισης και την υγεία της μπαταρίας σε Samsung Galaxy, χωρίς root και χωρίς υπολογιστή.

## Setup
Connect your phone to Wi-Fi, enable Developer options and Wireless debugging, then follow the app pairing instructions. Pull down the notifications, tap the arrow on the Battery Cycles notification, tap "Code" and type the six-digit code. No computer is required. Wireless debugging must be available for fresh readings; cached readings are labelled with their timestamp.

## Limitations
The Samsung usage counter divided by 100 is an estimate, not a manufacturer-certified cycle count. Tested readings came from an S23 Ultra. Other firmware may not expose the fields. End-to-end wireless pairing without a computer was verified on 2026-11-13 on a Galaxy S23 Ultra (SM-S918B, Android 17), from a clean install through pairing to a cycle reading. It has not been tested on other models; report problems through Issues.

## Build
Install JDK 17 and Android SDK platform 36. Set ANDROID_HOME, then run `./gradlew assembleDebug` or `./gradlew assembleRelease`. Release builds are unsigned without a private keystore.properties file. Signing keys are never included in this repository.

## Privacy
Battery readings and pairing credentials are stored on the device. No advertising or analytics. See the [privacy policy](PRIVACY.md).

## License
Project code: Apache-2.0. Dependencies retain their own licenses; see THIRD_PARTY_NOTICES.md.
