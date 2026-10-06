# Raabta Phone

Raabta Phone is a combined Android phone and contacts app maintained by Usama. It includes a dialer, call history, contact viewing and editing, favorites, groups, SIM selection, and configurable navigation. The current source version is `2.5.4-Full`.

## Build

This repository contains the complete Android Gradle source for the combined app. With JDK 17 and an Android SDK configured, run:

```powershell
.\gradlew.bat :app:assembleRabtaFull
```

The debug APK is generated under `app/build/outputs/apk/rabta/full/`. Build outputs, local SDK paths, caches, and signing keys are intentionally not committed. The Android package is `org.rabta.phone.classic.debug`; because this differs from previous editions, it installs separately rather than updating them.

## Project history and license

This repository starts with a clean Raabta source snapshot. The earlier combined working tree was not under version control, so previous local APKs do not correspond to recoverable Raabta source commits.

Raabta Phone includes modified GPL-3.0 components. See [NOTICE.md](NOTICE.md) for source attribution and legal notices.
