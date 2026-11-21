# Sift

An offline SMS app for Android that sorts your inbox, turns bank messages into a spending view, and keeps OTPs out of the way.

## What it does

Sift takes over as your default SMS app. Incoming messages are sorted into Important, Promotional, Personal, OTP, Unknown and Spam by a rules engine of plain JSON regexes, so every filing decision is a rule you can read and edit. Bank and card messages are parsed into a finance view of accounts, cards and spend, with bills and due dates in a separate alerts tab.

All of it runs on the device.

- SMS and MMS send and receive, dual-SIM, scheduled sending, per-thread drafts.
- Transparent categorisation — every rule is inspectable JSON, no ML black box.
- Finance dashboard, balance tracking and bill reminders built from bank SMS.
- OTP notifications with copy, optional auto-copy and auto-delete.
- A Spam folder that keeps bulk marketing out of the inbox: one-tap Report spam and Not spam, and a confirmed Empty spam. Nothing is deleted automatically.
- Blocked senders and keywords, a 30-day recycle bin, local backup and restore.

## Privacy

Sift is fully offline: the app does not request the `INTERNET` permission and ships no HTTP client. No telemetry, no analytics, no crash reporting, no Play Services. Parsing, categorisation and search happen on the device, and backups are local files you choose the location of. The single network operation in the app is the one MMS cannot avoid — retrieving a picture message is a transaction the Android system's own MMS service performs with your carrier, over the carrier network, with no third party involved.

## Build

Requires JDK 17 and the Android SDK with platform 35. Gradle 9.7.1 comes from the wrapper; the build uses AGP 9.1.1 and Kotlin 2.3.21, with `compileSdk` 35 and `minSdk` 23.

```bash
# if ANDROID_HOME is not set
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`. `./gradlew assembleRelease` produces a single R8-shrunk universal APK, unsigned unless the `SIGNING_*` environment variables are set. Checks:

```bash
./gradlew ktlintCheck lintDebug testDebugUnitTest
```

## Attribution

Sift is a fork of [Clear SMS](https://github.com/itsluminous/ClearSMS), copyright the Clear SMS contributors, licensed under the Apache License 2.0. The upstream copyright and third-party attribution notices are retained in [LICENSE](LICENSE) and [NOTICE](NOTICE). Modifications from the original are described in [CHANGES.md](CHANGES.md).

## Licence

Apache License 2.0. See [LICENSE](LICENSE).
