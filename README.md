# 🎬 Videora — Open-Source Local Video Editor for TikTok, Shorts & Reels

> **100% Local • Zero Sign-ups • Zero Cloud Uploads • No Watermarks • Apache 2.0 Open Source**

**Videora** is a privacy-first, open-source video editing app built specifically for creators making vertical short-form content for **TikTok**, **YouTube Shorts**, and **Instagram Reels**.

Unlike commercial video editors that demand cloud accounts, email registrations, subscription paywalls, and telemetry trackers, Videora runs **completely on-device**. Every frame is decoded, filtered, trimmed, and encoded locally using your Android device's hardware GPU via AndroidX Media3.

---

## 🚀 Quick Download

Download the signed, installable APK from the [Latest GitHub Release](https://github.com/sqrlwallet/videora/releases/latest) (only **~3.6 MB**):

| Release | Version | Size | Highlights |
| :--- | :--- | :--- | :--- |
| **Latest** | **v1.2.3** | **3.6 MB** | Layout constraint fix (bounded timeline height, 9:16 viewport restoration), legacy URI auto-caching |
| Previous | v1.2.2 | 3.6 MB | TextureView preview fix, NLE scrollable filmstrip, video thumbnails, fit/fill toggle, 77% size reduction |
| Previous | v1.1.0 | 15.2 MB | Multi-level undo/redo, voiceover studio, synthesized soundboard, emoji stickers |

---

## ✨ Features Tailored for Short-Form Creators

### 🎞️ 1. Professional Scrollable NLE Filmstrip Timeline
Modeled after **CapCut, Instagram Reels, and TikTok**:
- **Proportional Time Scale**: 48 dp per second. Every clip is physically sized by its duration—**no stretched clips**.
- **Real Video Frame Thumbnails**: Video frame thumbnails rendered directly across the filmstrip via Coil's `VideoFrameDecoder`.
- **Fluid Kinetic Scrolling**: Swipe, flick, and scroll horizontally across the timeline with smooth 60 FPS physics.
- **Center Magnetic Playhead**: Champagne gold playhead needle with glowing diamond head fixed in the center while the filmstrip smoothly glides beneath it.
- **Visual Timecode Ruler**: Millisecond tick marks across the timeline with monospace timecode display.

### 📱 2. Platform-Accurate Safe Zone Overlays & Fit/Fill Modes
Never let critical text hooks, face details, or stickers get blocked by platform buttons:
- **TikTok Safe Zone**: Live simulation of TikTok's right rail buttons (Avatar with `+`, Like, Comments, Bookmark, Share, Rotating Sound Disc) and bottom username/captions.
- **Instagram Reels Safe Zone**: Real-world overlay of Reels action icons, audio ticker, and account handle.
- **YouTube Shorts Safe Zone**: Overlay of Shorts thumbs up/down, comments, remix icon, and channel subscribe bar.
- **Fit vs. Fill Toggle**: One-tap toggle between **Fill** (9:16 vertical full crop) and **Fit** (100% video visible with letterboxing).

### ⏹️ 3. Playback Lifecycle & Battery Management
- **Automatic Pause on Background**: Playback stops immediately when you switch apps, minimize, lock your phone, or navigate away.
- **Audio Focus**: Automatically pauses when music (Spotify, YouTube) or calls occur.
- **Play-to-Stop**: Video halts and resets to start when playback finishes instead of looping infinitely.

### ⏱️ 4. Multi-Clip Timeline & Precision Editing
- **Undo / Redo History**: Multi-level state history (↶ / ↷) to revert or reapply any trimming, splitting, speed, or filtering changes.
- **Clip Reordering**: Move clips backward or forward along the timeline with one tap (`Move ◀` / `Move ▶`).
- **Mirror & Rotate**: Horizontal selfie-camera flip and 90° incremental rotations.
- **Zero-Latency Local Caching**: Selected clips are stored locally for instant scrubbing and zero permission revocation issues.
- **Accurate Trimming**: Millisecond-precision start and end trim sliders with live visual feedback.
- **Split at Playhead**: Instantly split any video clip at the current playhead cursor with a single tap.
- **Speed Control**: `0.25x` (Super Slo-Mo), `0.5x`, `0.75x`, `1.0x` (Normal), `1.25x`, `1.5x`, `2.0x`, `3.0x` (Hyperlapse).
- **Audio Mute**: Mute original video audio to replace with background music or voiceovers.

### 🎙️ 5. Built-in Sound Effects & Voiceover Studio
- **Voiceover Studio**: Record live microphone audio commentary synced directly to the video playhead with timer and pulsing indicator.
- **Built-in Sound Effects**: On-device synthesized soundboard (100% offline, zero network requests):
  - 💨 **Fast Whoosh**: Scene transition sweep
  - 🫧 **Bubble Pop**: UI tap & element pop
  - 🔔 **Bell Ding**: Highlight point chime
  - 💥 **Dramatic Bass Boom**: Sub-bass impact
  - 🪙 **Success Ding**: Gamified coin sound
  - 📸 **Camera Snap**: Flash snapshot click
- **Device Music**: Add local `.mp3`, `.wav`, or `.m4a` music tracks with independent volume sliders.

### 🪝 6. Viral Text Hooks, Templates & Emoji Stickers
- **Viral Hook Templates**: One-tap hook titles (*"POV: Nobody is talking about this 🤫"*, *"Wait until the end... 😱"*, *"Stop scrolling right now! 🛑"*, *"3 secrets you didn't know 💡"*).
- **Reaction Stickers & Emojis**: Tap to place trending reaction stickers (🔥, 💀, 😱, 🚀, 💯, ⚡, 🎯, 👍, 🛑, 🤫, etc.) directly on the video canvas.
- **TikTok Yellow Hook Pill**: High-contrast black pill with vibrant yellow bold text — the quintessential viral short-form hook format.
- **Modern White Pill, Neon Glow & Meme Impact**: Diverse creator typography presets.
- **Draggable Canvas & Timing**: Drag overlays anywhere on screen and configure start/end timestamps.

### 🎨 7. Hardware-Accelerated Color Grading & Filters
Real-time GPU shader matrices:
- **Vibrant Pop**: High-saturation contrast pop calibrated for vibrant OLED phone displays.
- **Golden Sunset**: Warm lifestyle/vlog golden hour lighting.
- **Cinematic Teal**: Cool shadows and cinematic highlights.
- **Retro Film**: Nostalgic sepia warmth.
- **Cyberpunk**: Dramatic neon color shifts.
- **Noir B&W**: Classic high-contrast black-and-white.

### ⚡ 8. Hardware Export Engine (AndroidX Media3 Transformer)
- Fast hardware-accelerated H.264 / AAC encoding.
- **Platform Presets**:
  - `TikTok Recommended`: 1080x1920 @ 30fps (optimal bitrate)
  - `YouTube Shorts HQ`: 1080x1920 @ 60fps (max clarity)
  - `Instagram Reels`: 1080x1920 @ 30fps (compression optimized)
  - `Quick Draft`: 720x1280 @ 30fps (fast preview export)
- Direct save to device `Movies/Videora/` and auto-indexing in Android MediaStore / Google Photos.
- Direct one-tap **"Share to TikTok / Reels / Shorts"** using Android's native share sheet.

### 🔒 9. 100% Local & Privacy-First
- **No Sign-ups**: Never asks for an email, password, phone number, or login.
- **No Cloud Uploads**: Clips, audios, and exports never leave your hardware.
- **Zero Telemetry**: No third-party trackers, analytics, or behavioral cookies.
- **Instant Demo Clip Generator**: Generates synthetic 9:16 video clips on-device using `MediaCodec` + `MediaMuxer` so you can test editing immediately even without camera roll videos.

---

## 🏗️ Architecture & Tech Stack

```
com.kevan.videorabykevan/
├── model/                     # Pure data classes & presets
│   ├── AspectRatioPreset.kt   # 9:16, 1:1, 4:5, 16:9
│   ├── SafeZoneType.kt        # TikTok, Reels, Shorts UI guides
│   ├── VideoClip.kt           # Timeline clip model (trim, speed, flip, rotate)
│   ├── AudioTrack.kt          # Background soundtrack & voiceover model
│   ├── TextOverlay.kt         # Viral hook text, templates & stickers
│   ├── VideoFilter.kt         # Color matrices & contrast presets
│   ├── ExportPreset.kt        # Hardware transcode profiles
│   └── Project.kt             # Project draft entity
├── data/
│   ├── BuiltInAudioHelper.kt  # On-device synthesized sound effects (Whoosh, Pop, Bell, etc.)
│   ├── VoiceRecorderHelper.kt # Local microphone voiceover recorder
│   ├── ProjectRepository.kt   # Local JSON persistence (100% offline)
│   └── SampleMediaHelper.kt   # Hardware MediaCodec on-device video generator
├── engine/                    # Video decoding, playback & export pipeline
│   ├── VideoPlayerManager.kt  # AndroidX Media3 ExoPlayer playlist & timeline sync
│   └── VideoExportManager.kt  # AndroidX Media3 Transformer GPU pipeline & MediaStore saver
├── ui/
│   ├── theme/                 # Dark Creator OLED palette & Material 3 theme
│   ├── components/            # Reusable UI components
│   │   ├── VideoPlayerView.kt # 9:16 surface with overlays & transport
│   │   ├── SafeZoneOverlay.kt # Platform UI bounds simulation
│   │   ├── TextOverlayCanvas.kt # Draggable text hook & sticker layer
│   │   ├── TimelineView.kt    # Multi-clip timeline with real-time playhead
│   │   ├── ClipActionRow.kt   # Fast action buttons (Trim, Speed, Reorder, Mirror, etc.)
│   │   ├── TrimSheet.kt       # Millisecond start/end trimmer
│   │   ├── SpeedSheet.kt      # Slo-mo / speedup presets
│   │   ├── FilterSheet.kt     # Color grading presets
│   │   ├── TextEditorSheet.kt # Hook editor, templates & typography
│   │   ├── StickerSheet.kt    # Reaction stickers & emojis
│   │   ├── VoiceoverSheet.kt  # Live voiceover recording studio
│   │   ├── AudioSheet.kt      # SFX soundboard, music & volume mixing
│   │   ├── ExportDialog.kt    # Encoding progress & social share sheet
│   │   └── OpenSourceSheet.kt # Transparency & manifesto dialog
│   ├── screens/
│   │   ├── HomeScreen.kt      # Drafts manager & platform tips
│   │   └── EditorScreen.kt    # Full editing studio with Undo/Redo
│   └── viewmodel/
│       ├── HomeViewModel.kt
│       └── EditorViewModel.kt
└── MainActivity.kt            # Compose entry point & smooth back-stack
```

---

## 🚀 Building & Running

```bash
# Clone the repository
git clone https://github.com/sqrlwallet/videora.git
cd videora

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Build signed release APK
./gradlew assembleRelease
```

---

## 📄 License

Videora is released under the **Apache License 2.0**.
See [LICENSE](LICENSE) for details. Free to use, modify, distribute, and study.
