# 2010sssTV

SSTV (Slow Scan Television) receive + transmit app for legacy Android (API 7-10 / Android 2.1-2.3.3), `om.sstvandroid2` v0.4.0.

## Features
- **Receive:** Robot36 decoder - live from microphone or decode a **WAV** file; scope + decoded image; saves PNG to `/sdcard/SSTV/`.
- **Transmit:** pick an image, **Play** over the speaker or **Save WAV** to `/sdcard/SSTV/` (default Robot 36). Live progress with unsent mask.
- **Image editor:** text (color/font/size/stroke), crop/rotate/flip, fit/letterbox or stretch to mode. Edits stay adjustable until you pick a new image (kept in RAM).

## Notes
- Permissions: `RECORD_AUDIO`, `WRITE_EXTERNAL_STORAGE` (Android 4.1+ may need mic permission granted in Settings).
- RX file input is **WAV only**.
- Built with the legacy `android-10` toolchain (no AndroidX/modern storage APIs).

## Credits and licensing
This project incorporates code from Apache License 2.0 licensed sources:
- **SSTV Encoder / TX components** - Copyright 2017 Olga Miller (<olga.rgb@gmail.com>), Apache License 2.0.
- **Robot36 / RX decoder and DSP components** - Copyright 2024 Ahmet Inan (<xdsopl@gmail.com>), Apache License 2.0.

See [`NOTICE`](NOTICE) for attribution details. A project `LICENSE` file may be added separately.