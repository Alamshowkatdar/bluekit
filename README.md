# BlueKit

BlueKit turns an Android phone into a wireless Bluetooth HID device. One phone
presents a keyboard, a trackpad/mouse, a gamepad and a media/presentation remote
to the host — no companion app on the PC, console or TV.

The UI is a dark "blueprint" theme built with Kotlin, Jetpack Compose and
Material 3. All four devices share one HID interface and one pairing.

## Modes

| Mode | What it sends | Report ID |
| --- | --- | --- |
| Keyboard | Boot-style 8-byte keyboard report, US QWERTY with a symbols page | 1 |
| Trackpad | 4-byte mouse report: buttons, relative X/Y, wheel | 2 |
| Media | 16-bit Consumer Control usage (playback, volume, AC Home/Back) | 3 |
| Gamepad | 16 buttons, 8-way hat, two sticks and two analog triggers | 4 |

Presentation keys (`PageUp`, `PageDown`, `F5`, `Esc`, arrows) are sent through
the keyboard report, so they work in any slideshow app.

## Build

Requirements:

- JDK 17 or newer
- Android SDK with platform 37 and build-tools 36.0.0

```bash
export JAVA_HOME=/opt/android-studio/jbr
export ANDROID_HOME=/home/meigo/Android/Sdk
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

`local.properties` points at the local SDK and is intentionally git-ignored.
Change it (or use `ANDROID_HOME`) for your machine.

## Use

1. Open BlueKit and grant Bluetooth permission.
2. Tap **Discoverable** and leave BlueKit open.
3. On the host, open Bluetooth settings and pair with this phone.
4. Pick a mode. Reports are sent as soon as a host is connected.

If the host was already paired, it can also be selected under **Paired hosts**.
Some hosts only connect when they initiate pairing, so the phone-side
**Connect** button is best-effort.

## Compatibility

BlueKit needs a phone that exposes the Android Bluetooth HID **Device** profile
(`BluetoothHidDevice`). Most Android phones running Android 9+ do. It works with
Windows, Linux, Android hosts and most consoles and TVs.

Known limitations:

- Apple hosts (macOS, iOS, iPadOS) do not support the HID device role used here.
- Some vendor ROMs remove or block the HID device profile.
- Only one app can hold the HID device profile at a time.
- The host must be discoverable from the phone only if it initiates pairing;
  BlueKit deliberately does not scan for nearby devices.

## Project layout

```
app/src/main/java/com/alamshowkat/bluekit/
  MainActivity.kt          permissions, Bluetooth intents, mode navigation
  BlueKitViewModel.kt      input state and report dispatch
  hid/HidDescriptor.kt     combined HID report descriptor
  hid/HidReports.kt        usages and report encoders
  hid/HidController.kt     Bluetooth HID profile lifecycle
  ui/                      Compose screens and shared components
```

## Licence and provenance

BlueKit is an original, clean-room implementation. It was inspired by the
feature set of [arnav-kr/Bluke](https://github.com/arnav-kr/Bluke), which is
licensed under AGPL-3.0, but no Bluke source code is copied or derived here.
