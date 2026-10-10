# 🌐 Digi Browser v2.0.0

**A lightweight, remote-friendly web browser for Android TV, tablets, and phones.** Built 100% with Kotlin + Jetpack Compose on top of the system WebView.

![Version](https://img.shields.io/badge/version-v2.0.0-green?style=flat-square)
![Platform](https://img.shields.io/badge/platform-Android%20TV%20%7C%20Phone%20%7C%20Tablet-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-23-blue)
![Target SDK](https://img.shields.io/badge/targetSdk-37-blue)
![Gradle](https://img.shields.io/badge/Gradle-9.6.0-02303A?logo=gradle&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?logo=jetpackcompose&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-ExoPlayer-orange)

<p>
  <img src="docs/home.png" width="48%" alt="Home" />
  <img src="docs/tabs.png" width="48%" alt="Tab switcher" />
</p>

---

## ✨ Why Digi Browser?

Most mobile browsers are made for fingers. Digi Browser is made for **the couch**:
a living-room browser you can drive with nothing but a TV remote, that still feels
at home on a phone.

| | |
|---|---|
| 🎮 **Remote-first & Ultra-precise** | Full D-pad navigation, focus rings everywhere, and a **high-precision on-screen cursor** (fine-tuned 10px step & smooth acceleration) to easily click any link on any website. |
| 🛡️ **Private DNS Protection** | Native support for Encrypted Private DNS providers (**AdGuard DNS**, **Cloudflare 1.1.1.1**, **Google Public DNS**, **Quad9**), plus quick access to System Network Settings. |
| 🎬 **Native video player** | Detects the video a page is playing and offers to open it in a built-in Media3 player with quality, subtitles, speed and full remote control. |
| 📱 **Adaptive layout** | One codebase, two layouts: sidebar-drawer on phones, wide TV layout with a tabs button in the header on large screens. |
| ⚡ **Instant tab switching** | Tabs keep their own live WebView (LRU pool), so switching never reloads the page, while memory stays bounded. |
| 🧠 **Memory-aware** | Background tabs are saved and released automatically when RAM is low (fewer live WebViews on TV / low-RAM devices). |
| 🛡️ **Safe defaults** | Invalid HTTPS certificates are blocked, pop-ups and ads/trackers can be blocked, WebView debugging is debug-builds only. |

---

## 🚀 Features

### Browsing & Cursor Control
- Search or type a URL (auto-detects URL vs. search query)
- Search engines: **Google, Bing, Yahoo, DuckDuckGo**
- **Ultra-fine TV Cursor**: Optimized D-pad movement step (10px per click) and smooth hold acceleration curve for effortless links selection on TV screens
- Back / Forward / Reload / Home control panel (opens with the remote's **Back** key)
- **Desktop / Mobile** site toggle per session, remembered
- Secure / not-secure indicator (HTTPS vs. HTTP)
- Error page with the real reason instead of a blank screen
- **Page fullscreen**: in-page players (including DRM sites that run inside the WebView) get a real full-screen view with system bars hidden

### Privacy & Security
- 🛡️ **Private DNS Support**: Choose between:
  - **AdGuard DNS**: Blocks ads, trackers, and malicious domains
  - **Cloudflare DNS (1.1.1.1)**: Fast, privacy-focused public DNS
  - **Google Public DNS (8.8.8.8)**: High-performance global DNS
  - **Quad9 DNS**: Blocks malware and phishing threats
  - **Off**: Uses default system network DNS
  - *Direct shortcut to open Android System Private DNS Settings*
- Host-based **ad & tracker blocker**
- **Strong ad blocking** (separate toggle, on by default): pop-under / push / native-ad networks, third-party ad URL patterns, hidden ad containers, `window.open` without a user gesture
- **Pop-up blocker**
- JavaScript toggle, history toggle

### Tabs
- Tab switcher with live **previews** (4:3 cards), `CLEAR · ⊕ · DONE` pill
- Tabs are **saved automatically** and restored after the app is closed
- Same tab list on Home, sidebar, and Browser (single source of truth)
- Up to **20 tabs**, with an LRU pool of 2–3 live WebViews

### 🎬 Native video player
When a page loads a video, Digi Browser shows a **"Video detected"** card:
**Play in app** or **Play in browser**. The player is a full-screen Compose screen
powered by **Media3 / ExoPlayer**.

- **Detection**: HLS (`.m3u8`), DASH (`.mpd`), progressive files (`.mp4 .m4v .webm .mkv .mov .ogv .3gp`) and side-loadable subtitles (`.vtt`, `.srt`)
- **Main video vs. ads**: manifests are probed in the background (duration, resolution, live), clips under 60 s are treated as ads/previews
- **Stream list**: a button inside the player lists *every* stream found on the page with thumbnail, file name, URL, size, duration and type
- **Same identity as the browser**: `User-Agent`, `Referer`, `Origin` and per-request **cookies** from the WebView are forwarded
- **Quality**: Auto + every available resolution, switchable while playing
- **Subtitles**: embedded tracks and side-loaded `.vtt`/`.srt`, with a language picker and **timing nudge** (±0.1 s; side-loaded files only)
- **Extras**: speed 0.5×–2×, aspect modes (Fit / Fill / Zoom / 16:9 / 4:3), **resume** from last position, **Picture-in-Picture** (Android 8+, including Android TV)

### App updates
- **Settings → About** shows the installed version (v2.0.0) with a live status chip: *checking · up to date · update available (vX.Y.Z)*
- Opening the version row opens the **update page**: installed vs latest version, **release notes from GitHub** (rendered Markdown), and one-tap **download + install** of the release APK

### Library
- ⭐ **Bookmarks**, 🕘 **History** (can be turned off), ⬇️ **Downloads** (with install/open for APKs)
- Home **shortcuts**: add, edit, reorder, delete, with custom emoji and color

---

## 🎮 Controls

### Browser

| Input | Action |
|---|---|
| D-pad | Move the on-screen cursor (fine 10px step) / move focus |
| OK / Enter | Click under the cursor / activate focused item |
| Back | Open the control panel · Back again closes it · with no history returns Home |
| Panel → ⌃ tabs hint | Focus the hint and press OK/↓ to open the tab switcher |
| Tab switcher | D-pad to move · OK to open · `CLEAR` closes all · `+` new tab |
| Video card | OK on **Play in app** / **Play in browser** · Back dismisses it · panel **Video** button brings it back |
| Touch | Normal WebView gestures, drawer menu on phones |

### Video player

| Input | Action |
|---|---|
| ← / → | Seek ±10 s (hold: ±30 s) |
| OK / Enter | Play / pause |
| ↑ / ↓ | Show controls and move between them |
| Media keys | Play, pause, fast-forward, rewind, stop always work |
| Back | Close the open menu, then leave the player |
| Touch | Tap = show/hide controls · double-tap left/right = ∓10 s · drag the seek bar |

---

## 🛠️ Build & run

**Requirements**

- Android Studio (2026.1+ / AGP 9.4+ support)
- Gradle 9.6.0 (configured in `gradle/wrapper/gradle-wrapper.properties`)
- JDK 17+
- Android SDK 37

```bash
git clone https://github.com/Appsventory/Digi-TvBrowser.git
cd Digi-TvBrowser
./gradlew assembleDebug          # debug APK
./gradlew installDebug           # install on a connected device / emulator
```

Run on an **Android TV emulator** (API 34+) or a real TV via `adb connect <tv-ip>:<port>`.

---

## 🔐 Permissions

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Browsing, video playback and update checks |
| `WRITE_EXTERNAL_STORAGE` (Android ≤ 9 only) | Saving downloads on old devices |
| `REQUEST_INSTALL_PACKAGES` | Install a downloaded APK when you tap it |

---

## 🤝 Contributing

Issues and pull requests are welcome.

---

**Made with [♥︎] by Appsventory & ICK Network Team**
