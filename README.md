# Raabta Phone

Raabta Phone is a combined Android phone and contacts app maintained by Usama. The current source version is `2.6.0-Full`. One APK provides the dialer, in-call screen, call history, and full Contacts interface. Android 8.0 (API 26) or later is required.

## Features

### Calling and call history

- Dialpad with contact suggestions, keypad/T9 name matching, speed dial, optional key tones and vibration, and an option to open the dialpad at launch.
- Incoming and ongoing call screens with answer/decline, mute, hold/resume, keypad tones, audio-route selection, and supported conference-call controls.
- Optional call confirmation, swipe-to-answer behavior, proximity-sensor behavior, and full-screen call display.
- Recents with call details, missed calls, optional grouping of consecutive calls, number copying, SMS, contact lookup, and adding a number to contacts. Call-history import and export use JSON files.
- Dual-SIM call chooser with SIM slot, name, and carrier/network when Android provides them. A SIM can be remembered for a particular phone number and that preference can be removed.

### Contacts

- Complete Contacts screen in the same APK: view, create, edit, delete, share, and favorite contacts; manage groups; and filter or sort contacts.
- Contact details support phone numbers, email addresses, postal addresses, IM accounts, events, websites, notes, organization, groups, contact sources, photos, and custom ringtones where supported by the contact account.
- Phone numbers are given room to display in contact details without a competing “Mobile” label; long-press a number to copy it. Email rows are hidden by default, but can be restored through **Manage visible fields**. Hiding a field does not delete its data.
- Configurable contact tabs: **Keypad, Recents, Contacts** by default, with optional **Favorites** and **Groups**. Tabs can be shown/hidden, and the startup tab can be selected.
- Optional contact thumbnails, phone-number display and formatting, contacts-with-numbers-only filtering, surname-first names, duplicate-contact merging, and call confirmation.
- Import and export contacts as vCard/VCF files. Optional automatic contact backups can be configured on supported Android versions.
- Optional SMS and WhatsApp shortcuts and favorites-first search results. WhatsApp links require a number saved with its country code.

### Search designed for older users

**Classic search is on by default** in both Phone and Contacts. It addresses the confusing behavior where typing the beginning of a person's name could return unrelated contacts merely because the same letters appeared in a surname, note, email, or other hidden field.

- A text query matches the **beginning of the displayed name**, ignoring case, accents, and extra whitespace. It does not match a middle-of-name substring or hidden fields.
- A number query matches the **beginning of a phone number** after formatting characters are removed. On the dialpad, number keys can also match the beginning of a contact's name through T9.
- Example: searching `Ali` finds a displayed name starting with Ali, not a contact whose name contains `ali` only in the middle. Searching `030` finds numbers beginning with `030`, not numbers that contain `030` later.
- The **Classic search** switch in Phone settings turns this behavior off if broader substring and additional-field searching is preferred. Search is also available in the integrated Contacts interface.

### Personalization

- Choose font size (small, medium, large, or extra large) instead of forcing large text by default; customize colors and app language where Android supports it.
- Configure visible Phone tabs, startup tab, what tapping a contact does, dialpad number display, speed dial, and phone-number formatting.
- A Credits page identifies Usama and retains the required open-source attribution.

## Build

This repository contains the complete Android Gradle source for the combined app. With JDK 17 and an Android SDK configured, run:

```powershell
.\gradlew.bat :app:assembleRabtaFull
```

The debug-signed APK is generated under `app/build/outputs/apk/rabta/full/`. Build outputs, local SDK paths, caches, and signing keys are intentionally not committed. The Android package is `org.rabta.phone.classic.debug`; because this differs from previous editions, it installs separately rather than updating them. Select Raabta Phone as the default Phone app in Android settings to use its in-call interface.

## Project history and license

This repository starts with a clean Raabta source snapshot. The earlier combined working tree was not under version control, so previous local APKs do not correspond to recoverable Raabta source commits.

Raabta Phone includes modified GPL-3.0 components. See [NOTICE.md](NOTICE.md) for source attribution and legal notices.
