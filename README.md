# Watch Music for CMF Watch Pro 2

Control your phone's music, podcasts and media from your wrist. Watch Music is a custom app for the CMF Watch Pro 2, with a companion Android app that does the work on the phone.

> **Unofficial project.** Not affiliated with, endorsed by or supported by CMF, Nothing Technology Limited or any related company. All names and trademarks belong to their owners.

> **Risk.** Installing apps on the watch uses patched firmware and sideloading. It can break your watch, lose data and void your warranty. You do this at your own risk. See [Risks](#risks).

## What it does

- **Music** – a smooth song list (crown or drag) and a player with artist, song, previous / play / next, a progress ring and a soft background in the album's colours.
- **Podcasts** – every podcast you added in the phone app is a row; tap it to open its player, previous / next moves between episodes.
- **Control media** – control whatever is playing on the phone (music or video apps), with the same colour treatment when artwork is available.
- **Custom** – run your saved phone shortcuts from the watch.
- **Volume** – press the speaker button, then turn the crown. It switches itself off after 30 seconds.

The start screen shows only the categories that have content, and opens straight into the controls when something is playing.

## Downloads

Get these from the [Releases](../../releases) page:

| File | What it is |
|---|---|
| `WatchMenu.apk` | The Android companion app (**Watch Menu 2**) |
| `watch-music.wp2app` | The watch app |
| `watch-sideload.html` | The page that installs the watch app (open it in Chrome or Edge) |

## You need

- A **CMF Watch Pro 2** with the **patched firmware** and the **App Loader** (ABI 1.2 or newer) installed. See the [firmware](/firmware) folder.
- An Android phone.
- **Chrome or Edge** on a computer (or Android Chrome) – the install page uses Web Bluetooth.

## Install

1. **Phone app.** Download `WatchMenu.apk` and install it. Allow "install unknown apps" if Android asks. Open the app and grant the permissions it asks for (including notification access, so it can see what is playing).
2. **Pair the watch** in the phone app and add your music and podcasts.
3. **Free the watch.** The watch talks to one device at a time. Turn Bluetooth off on the phone the watch is paired to, and wake the watch screen.
4. **Open `watch-sideload.html`** in Chrome or Edge.
5. Tick the risk box and press **Connect watch**. Pick your watch in the list. If the watch shows "Paired", leave it alone.
6. **Drag and drop `watch-music.wp2app`** onto the page (or tap to choose it) and press **Install**. Keep the watch awake and close until it says it is installed.
7. **Turn the phone's Bluetooth back on**, open Watch Music on the watch, and it connects to the phone app.

To update, repeat steps 3–6 with the new `.wp2app`. To remove the app, use the Remove button in the "On the watch" list.

## Troubleshooting

- **Watch not in the list** – press "Show every device" on the page.
- **"Paired" popup on the watch** – leave it and try again.
- **No Bluetooth in the browser** – use Chrome or Edge. On Linux enable `chrome://flags/#enable-web-bluetooth` and restart.
- **Install stops halfway** – wake the watch, keep it close, and try again.
- **App opens slowly or shows nothing** – make sure the phone app and watch app are the same release, and reconnect the watch once.

## Risks

- Sideloading and patched firmware are not supported by the manufacturer. Things can go wrong, including a watch that no longer starts.
- Your warranty may be void.
- Charge the watch before you start and do not interrupt an install.
- This software is provided "as is", without warranty of any kind. The authors are not responsible for any damage or data loss.
- Use it only on your own devices.

## Credits and licence

- The phone app and install page use the Nothing-style fonts NDot 55 and NType 82. Check their licence before you redistribute them.
- Licence: *add your licence here (for example MIT) and a `LICENSE` file.*
