# Native video player

Flow: `shouldInterceptRequest` + page JS hook → `MediaStreamDetector` → `ManifestProbe` → `StreamRanking`
→ `StreamPrompt` → `VideoPlayerScreen` (Media3, same window, no new Activity).

## Detection
- Network: `.m3u8`, `.mpd`, `.mp4/.m4v/.webm/.mkv/.mov/.ogv/.3gp`; `.vtt/.srt` as subtitles (HLS segment-like names are ignored).
- DOM (needs JavaScript on): `currentSrc`, `src`, `<source src>`, `<track src srclang>` of every `<video>`, in the top document and same-origin iframes (`PlayerConfig.VIDEO_HOOK_JS`, bridge `DigiVideo`).
- Only the active tab is watched; detections reset on every page load / tab switch.
- With strong ad blocking on, URLs that look like ads (VAST, preroll …) are not listed.

## Main video vs ads
- `ManifestProbe` reads HLS/DASH manifests: duration, tallest rendition, live.
- `StreamRanking`: matches the page's playing `<video>` duration → long VOD → live → unknown → pending → short (< 60 s).
- The prompt appears when at least one stream is eligible; the panel **Video** button forces it.

## Playback
- Headers: User-Agent, Referer, Origin from the request; Cookie is read per request URL from the WebView cookie jar (`PlayerFactory`).
- The in-player **Streams** button lists everything detected (thumbnail, file name, URL, size, duration) and switches without leaving the player.
- Remote: ←/→ seek (hold = 30 s), OK play/pause, ↑/↓ show controls and move between them, Back closes menu then player.
- Side-loaded subtitles are drawn by the app (timing nudge); embedded tracks use ExoPlayer and cannot be nudged.
- Resume positions: `Prefs` (100 newest, keyed by host+path).
- PiP: manifest `supportsPictureInPicture`, Home key → `MainActivity.onUserLeaveHint`.

## Page fullscreen
`WebChromeClient.onShowCustomView` → `FullscreenWebVideo` (immersive, landscape on phones). DRM / MSE players stay in the page.
