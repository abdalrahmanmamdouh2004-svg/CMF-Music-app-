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

## Firmware Installation

Before installing Watch Music, you need to patch your watch using the FMC Tool and install the Uploader.

1. Open the [FMC Tool website](https://fmc.tools/firmware).
2. Follow the instructions to patch your **CMF Watch Pro 2**.
3. Install the **app loader** on your watch.
4. Once both steps are complete, you can continue with the [Installation](#installation) instructions below.

> **Note:** This preparation is required before installing Watch Music.

## Installation

### 1. Download the required files

Download both files:

- `watch-sideload.html` — the watch installation page.
- `watch-music.wp2app` — the Watch Music app for your watch.

### 2. Install Watch Music on your watch

1. Open `watch-sideload.html` in **Google Chrome** or **Microsoft Edge**.
2. Tick the risk acknowledgment box and press **Connect watch**.
3. Select your **CMF Watch Pro 2** from the device list.
4. If the watch displays **"Paired"**, leave it as it is.
5. Drag and drop `watch-music.wp2app` onto the page.
6. Press **Install** and keep your watch awake and nearby until installation is complete.

### 3. Install the phone app

1. Download `WatchMenu.apk` and install it on your Android phone.
2. Allow installation from unknown sources if Android asks.
3. Open the app and grant the requested permissions, including **Notification Access**.

> **Important:** Watch Music cannot be used alongside the Nothing X app. Make sure the watch is disconnected from Nothing X before setting it up.

### 4. Add your music and podcasts

Open the phone app and pair your watch.

- **Music:** Copy a link to your favourite songs or share an entire playlist with the app.
- **Podcasts:** Copy the podcast's share link into the app, then choose the episode you want to listen to.

Once your content is added, turn Bluetooth back on if you disabled it during installation. Open Watch Music on your watch and it should connect to the phone app.

---

## Updating or Removing Watch Music

- **Update:** Repeat Step 2 using the new `watch-music.wp2app` file.
- **Remove:** Open the sideloading page, connect your watch, and use the **Remove** button in the *On the watch* list.

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
