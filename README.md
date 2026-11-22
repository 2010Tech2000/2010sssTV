# 2010sssTV
<img width="100%" height="auto" alt="20261122_135300" src="https://github.com/user-attachments/assets/a2a5809b-a75c-4402-8fea-efa17707c57f" />

<div align="right">
<img src="https://img.shields.io/badge/Android-2.1%2B-brightgreen.svg?style=flat&color=green" alt="Android 2.1+" />
  <a href="https://github.com/2010Tech2000/2010sssTV/releases">
    <img src="https://img.shields.io/github/release/2010Tech2000/2010sssTV?style=flat&color=green" />
  </a>
  <img src="https://img.shields.io/github/repo-size/2010Tech2000/2010sssTV?style=flat&color=lightgrey"/>
  <img src="https://img.shields.io/github/downloads/2010Tech2000/2010sssTV/total?style=flat&color=lightgrey" />
</div>

**2010sssTV** is an app which allows you to do SSTV stuff on a device with **android older than 4.0.3**. The app lets you to both **send and receive SSTV images** through the device's speaker/mic or aux jack (also probably with Bluetooth speakers/headphones). Also has a built in simple image editor to edit text, crop and rotate and stretch. A settings page with options to toggle image auto-save (when received) and setting the default locations to save the files, also setting a background color to your preference. (by the time of writing this) The app is around 150 kilobytes, uses android's available drawables and APIs, almost no preloaded assets in the app (biggest exception being the app icon). Previously there was not a single SSTV app which worked on android 2, me being a 2010s tech enthusiast, took it into my own hands and made my own app. Biggest downside of this project is that it's heavily vibecoded.

# Features
### Receive
- A live preview at the bottom half of the screen. Shows everything being heard by the mic.
- Above it an info area showing the current state, auto-detected mode and resolution.
- At the top right is a preview window, showing the cropped received image and its progress.
- **3 buttons:** Pause: pauses the decoding, does not clear the current pixels. WAV: Lets you pick .wav file to decode, useful if you made an audio recording on another app or device. Save: Saves the current decoded image from the image preview window. (if empty, a black image is saved). Not needed if you have auto-save on.

### Transmit
- At the top is the image container, show the image you're going to transmit/transmitting. When the page is opened, a placeholder image is inserted (your wallpaper cropped), a button to insert your image is in the center (disappears once you inserted an image). While transmitting a dark mask appears indicating the current status of transmitting.
- "Edit" button: Opens an in-app editor. Enabled only when an image is already inserted.
- "Replace" button: Lets you pick a new image. Enabled only when one is already inserted.
- "Play" button: Starts playing an encoded audio of the image.
- "Save WAV" button: Saves a .wav file at the location selected in settings.
- "Stop" button: Only visible when audio is playing. Replaces both play and save buttons, making it easy to press.
- Mode selector: lets you select a mode in which the image will be encoded.
- Status bar at the bottom: Shows the current % status of how much of the image was encoded.
- Under the status bar is info of the status, elapsed time and time left.

### Editing
contains 2 modes, Text and Crop/Rotate. Edited images are not saved to device. Unprocessed image is always saved on RAM letting you to continue editing it later (as long as the transmit page wasn't closed).
Text mode:
- an image at the top, text items will be displayed on top.
- A text field, probably lets you enter text idk.
- Next to the field are 2 buttons to add a text item, and to remove the currently selected one.
- Under those are font, color, size, stroke settings, all change the values of the current selected text item. Once a new text item is selected, its settings are shown on the settings pane.
Crop/Rotate mode:
- an image at the top, green crop lines.
Lets you crop, stretch, rotate or rotate an image. Aspect ratio is never changed as it depends on the encoding mode selected.
- Apply and cancel and revert buttons: applies the crop, cancel discards the current crop. Revert button undoes all the crops, including ones in all _sessions_ before.
- Row of buttons: Rotate -90 degrees, rotate 90 degrees, flip horizontally, flip vertically.
- Stretch to mode: sets the current image to get stretched to fully fit the current mode.
Cancel and done buttons, done button exits the editor, saves the _project_ into RAM. Cancel discards all new edits made on that _session_ and exits editor.

### Settings
The settings page has all the settings for the app:
- Auto-save: saves a decoded image once it's completed.
- "Received images folder": A field to enter a location that images will be saved at. Next to it is a button to open a location picker. Default is `/Pictures/SSTV`
- "Saved audio folder": A field to enter a location that audio files will be saved at. Next to it is a button to open a location picker. Default is `/Music/SSTV`
- App background: lets you choose a background color which will be used app-wide. Consists of: Oled - pitch black, oled friendly; Blue - matched to the android 2 title bar; Green; Purple; Light - grey, matched to android 4's title bar; Teal; Hell - Hellish dark red, for fun; "Discord" - uses a color from DC's color palette.
- Also a Custom color selector which for some reason is still there. I mean you can suggest color presets to be added.

## Features only here:
This app has code from other open source SSTV projects. The code has been ported to work on android 2, edited to be easier with added QOL changes. The app is in beta, but it already has its own features. **Compared to [SSTVEncoder2](https://github.com/olgamiller/SSTVEncoder2) and [Robot36](https://play.google.com/store/apps/details?id=xdsopl.robot36&hl=lt&pli=1).**
- Stopping encoding stops audio right at that moment. SSTVEncoder2 processed audio in large bits, and the current one continued playing until it was finished, sometimes taking up to a second or 2 to stop.
- A smoother encoding status bar, as a result it's more accurate. _Currently not fully, to be fixed_
Some other not so important features:
- An image preview window on receive page. Indicates how much of the image is left based on the modes aspect ratio.
- Themes. Robot36 (on play store) does have a better equivalent using material UI.
Expect more later!

## Plans:
- Make a version that works on modern android (including above 12). Using latest Material UI.
- Make a clone for J2ME (or at least symbian)
- support for opening more audio files.
- Refresh/overhaul the code to be cleaner and simpler.
- A mode for super weak devices so they could decode images at the right speed. The galaxy 5 is not capable enough, but a working demo has been achieved previously (around of making `v0.5`).
- Fix black bars on editor and get white outlines on TX.

## Credits and licensing
This project incorporates code from Apache License 2.0 licensed sources:
- **SSTV Encoder / TX components** - Copyright 2017 Olga Miller (<olga.rgb@gmail.com>), Apache License 2.0.
- **Robot36 / RX decoder and DSP components** - Copyright 2024 Ahmet Inan (<xdsopl@gmail.com>), Apache License 2.0.

See [`NOTICE`](NOTICE) for attribution details. A project `LICENSE` file may be added separately.

*2010sssTV <- 2010's SSTV <- 2010Tech's Slow-Scan Television.*
