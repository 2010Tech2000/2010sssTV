# 2010sssTV

An Android SSTV (Slow Scan Television) **receive + transmit** app for legacy devices.

- **Package:** `om.sstvandroid2`
- **Version:** 0.4.0 (`versionCode` 11)
- **Min SDK:** 7 (Android 2.1) · **Target SDK:** 10 (Android 2.3.3)
- **Tested on:** Samsung Galaxy S3 mini (GT-I8190). Built for Galaxy S1/Ace/S3 mini–class devices.

## Features
- **Receive (RX):** Robot36-based decoder. Live RX from microphone or decode a **WAV** file; scope + decoded image; save decoded image as PNG to `/sdcard/SSTV/`.
- **Transmit (TX):** SSTV encode/playback (SSTVEncoder2 modes, default Robot 36). Pick an image, **Play** audio over the speaker or **Save WAV** to `/sdcard/SSTV/`. Live progress with unsent-mask overlay.
- **Image editor (RAM, non-destructive while open):** add/move text (color/font/size/stroke), crop/rotate/flip, fit (letterbox, aspect preserved) or **Stretch to mode**. Reopen keeps text/edits adjustable; picking a new image clears edits. Editor state is in RAM (lost if the app/process is killed).

## Permissions
- `RECORD_AUDIO` — microphone receive.
- `WRITE_EXTERNAL_STORAGE` — save RX PNG / TX WAV to `/sdcard/SSTV/`.
- On Android 4.1+ you may need to grant microphone permission manually in system Settings.

## Notes / limits
- RX file input is **WAV only** (no MP3/FLAC decode on API 10).
- No AndroidX/support libraries, no modern storage/media APIs — classic Android views for API 7–10 compatibility.
- Release APK: `bin/2010sssTV-0.4.0.apk` (when built).

## Building (manual, legacy toolchain)
Requires Android SDK (`platforms/android-10`, `build-tools/23.0.1`) and JDK 8 (for `jarsigner`). Example outline:
1. `aapt package -f -m -M AndroidManifest.xml -S res -I <sdk>/platforms/android-10/android.jar -J gen`
2. Compile `src`+`gen` with `ecj -source 1.6 -target 1.6 -bootclasspath android-10/android.jar`
3. `dx --dex` → `classes.dex`
4. `aapt package` + `aapt add classes.dex` → unsigned APK, then `jarsigner` (v1, SHA1withRSA) → `zipalign`.

## License
No license file yet. Add a `LICENSE` before accepting outside contributions or redistributing source.
