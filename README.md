# 🎬 Videora — Open-Source Local Video Editor for TikTok, Shorts & Reels

> **100% Local • Zero Sign-ups • Zero Cloud Uploads • No Watermarks • Apache 2.0 Open Source**

**Videora** is a privacy-first, open-source video editing app built specifically for creators making vertical short-form content for **TikTok**, **YouTube Shorts**, and **Instagram Reels**.

Unlike commercial video editors that demand cloud accounts, email registrations, subscription paywalls, and telemetry trackers, Videora runs **completely on-device**. Every frame is decoded, filtered, trimmed, and encoded locally using your Android device's hardware GPU via AndroidX Media3.

---

## ✨ Features Tailored for Short-Form Creators

### 📱 1. Platform-Accurate Safe Zone Overlays
Never let critical text hooks, face details, or stickers get blocked by platform buttons:
- **TikTok Safe Zone**: Live simulation of TikTok's right rail buttons (Avatar with `+`, Like, Comments, Bookmark, Share, Rotating Sound Disc) and bottom username/captions.
- **Instagram Reels Safe Zone**: Real-world overlay of Reels action icons, audio ticker, and account handle.
- **YouTube Shorts Safe Zone**: Overlay of Shorts thumbs up/down, comments, remix icon, and channel subscribe bar.
- **Clean View**: One-tap toggle for an unobstructed canvas.

### ⏱️ 2. Multi-Clip Timeline & Precision Editing
- **Modern Media Picker**: Import multiple videos and photos from device gallery via Android PhotoPicker (no runtime storage permission required).
- **Accurate Trimming**: Millisecond-precision start and end trim sliders with live visual feedback.
- **Split at Playhead**: Instantly split any video clip at the current playhead cursor with a single tap.
- **Speed Control**: `0.5x` (Smooth Slow-Motion), `0.75x`, `1.0x` (Normal), `1.25x`, `1.5x`, `2.0x` (Timelapse).
- **Clip Duplication & Deletion**: Reorder and manage timeline clips effortlessly.
- **Audio Mute**: Mute original video audio to replace with background music or voiceovers.

### 🪝 3. Viral Text Hooks & Dynamic Captions
- **TikTok Yellow Hook Pill**: High-contrast black pill with vibrant yellow bold text — the quintessential viral short-form hook format.
- **Modern White Pill**: Translucent dark pill with crisp white typography.
- **Neon Glow & Cyber**: Electric cyan and magenta glowing text.
- **Meme Impact**: Bold white typography for high-impact punchlines.
- **Position Placement**: One-tap Top Hook (upper third), Center Title, or Lower Subtitle.
- **Draggable Canvas**: Drag and reposition text overlays anywhere on screen.
- **Timing Sliders**: Set exact start and end timestamps for each caption.

### 🎨 4. Hardware-Accelerated Color Grading & Filters
Real-time GPU shader matrices:
- **Vibrant Pop**: High-saturation contrast pop calibrated for vibrant OLED phone displays.
- **Golden Sunset**: Warm lifestyle/vlog golden hour lighting.
- **Cinematic Teal**: Cool shadows and cinematic highlights.
- **Retro Film**: Nostalgic sepia warmth.
- **Cyberpunk**: Dramatic neon color shifts.
- **Noir B&W**: Classic high-contrast black-and-white.

### 🎵 5. Audio & Background Music Mixing
- Add local audio tracks (`.mp3`, `.wav`, `.m4a`, `.ogg`) from device storage.
- Independent volume control (0% to 150%).
- One-tap toggle to mute original video background noise.

### ⚡ 6. Hardware Export Engine (AndroidX Media3 Transformer)
- Fast hardware-accelerated H.264 / AAC encoding.
- **Platform Presets**:
  - `TikTok Recommended`: 1080x1920 @ 30fps (optimal bitrate)
  - `YouTube Shorts HQ`: 1080x1920 @ 60fps (max clarity)
  - `Instagram Reels`: 1080x1920 @ 30fps (compression optimized)
  - `Quick Draft`: 720x1280 @ 30fps (fast preview export)
- Direct save to device `Movies/Videora/` and auto-indexing in Android MediaStore / Google Photos.
- Direct one-tap **"Share to TikTok / Reels / Shorts"** using Android's native share sheet.

### 🔒 7. 100% Local & Privacy-First
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
│   ├── VideoClip.kt           # Timeline clip model (trim, speed, volume)
│   ├── AudioTrack.kt          # Background soundtrack model
│   ├── TextOverlay.kt         # Viral hook text & styling presets
│   ├── VideoFilter.kt         # Color matrices & contrast presets
│   ├── ExportPreset.kt        # Hardware transcode profiles
│   └── Project.kt             # Project draft entity
├── data/
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
│   │   ├── TextOverlayCanvas.kt # Draggable text hook layer
│   │   ├── TimelineView.kt    # Multi-clip timeline with real-time playhead
│   │   ├── ClipActionRow.kt   # Fast action buttons
│   │   ├── TrimSheet.kt       # Millisecond start/end trimmer
│   │   ├── SpeedSheet.kt      # Slo-mo / speedup presets
│   │   ├── FilterSheet.kt     # Color grading presets
│   │   ├── TextEditorSheet.kt # Hook editor & typography
│   │   ├── AudioSheet.kt      # Soundtrack & volume mixing
│   │   ├── ExportDialog.kt    # Encoding progress & social share sheet
│   │   └── OpenSourceSheet.kt # Transparency & manifesto dialog
│   ├── screens/
│   │   ├── HomeScreen.kt      # Drafts manager & platform tips
│   │   └── EditorScreen.kt    # Full editing studio
│   └── viewmodel/
│       ├── HomeViewModel.kt
│       └── EditorViewModel.kt
└── MainActivity.kt            # Compose entry point & smooth back-stack
```

### Libraries Used:
- **Jetpack Compose + Material 3**: Modern, declarative UI with OLED dark theme.
- **AndroidX Media3 (1.5.1)**:
  - `media3-exoplayer`: High-performance playback, playlist clipping, and seeking.
  - `media3-transformer`: Hardware-accelerated GPU export pipeline.
  - `media3-effect`: Scaling, cropping, and RGB color matrices.
- **Gson**: Lightweight local JSON serialization.
- **Coil**: Image and thumbnail rendering.

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug / Meerkat / 2024.2+
- Android SDK 30+ (Compile SDK: 37)
- JDK 17+ (or bundled JetBrains Runtime)

### Command Line Build
```bash
# Clone the repository
git clone https://github.com/your-username/videora.git
cd videora

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Install to connected device or emulator
./gradlew installDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License

Videora is released under the **Apache License 2.0**.
See [LICENSE](LICENSE) for details. Free to use, modify, distribute, and study.
