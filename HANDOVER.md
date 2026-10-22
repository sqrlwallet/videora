# Videora: Agent Handover

Context for an agent continuing work on this repo. Read this before changing code.
Last updated 2026-10-22, at commit `0c3a458` on `main` (pushed to `origin`).

## 1. What the app is

Videora is an offline, privacy-first Android video editor for vertical short-form video
(TikTok / Reels / Shorts). It has no accounts, no network calls and no telemetry. Kotlin +
Jetpack Compose UI, AndroidX Media3 for playback (ExoPlayer) and export (Transformer).
About 9k lines of Kotlin in a single `app` module.

| Item | Value |
| --- | --- |
| Package | `com.kevan.videorabykevan` |
| minSdk / target / compile | 26 / 37 / 37 |
| Kotlin / AGP / Compose BOM | 2.2.10 / 9.4.1 / 2026.02.01 |
| Media3 | **1.5.1** (pinned; see gotchas) |
| Images | Coil 2.7.0 (+ `coil-video` for frame thumbnails) |
| Persistence | Gson 2.11.0, one JSON file |
| Version | `versionName 1.2.3`, `versionCode 7` (not bumped for the latest work) |
| Remote | `https://github.com/sqrlwallet/videora` |

## 2. Build and test

There is no `java` on PATH. Use Android Studio's bundled JDK:

```bash
export JAVA_HOME=/snap/android-studio/current/jbr
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

- Unit tests: `app/src/test/.../VideoraUnitTest.kt` and `ColorCorrectionTest.kt` (26 tests, all passing).
  They are plain JVM tests with no Robolectric. Code that touches `android.*` classes can't be unit tested,
  so keep logic in pure Kotlin where possible (as `ClipColorMatrix` and `ProjectJson` do).
- The release build uses R8 with `app/src/main/keepRules/rules.keep`. Everything in `model.**` is kept for Gson.
  Always build release too, because minification problems only show up there.
- **No emulator or device is available to the agent.** The owner tests on their own phone, so
  "builds and tests pass" is the most an agent can verify. Say clearly what is unverified at runtime.
- Media3 has no source jars here. To check whether an API exists in 1.5.1, unzip the AAR from
  `~/.gradle/caches/modules-2/files-2.1/androidx.media3/` and run
  `/snap/android-studio/current/jbr/bin/javap` on its `classes.jar`.

## 3. Architecture map

```
model/        Pure data classes, all Gson-serialized
  Project            clips, audioTracks, textOverlays, aspectRatio, safeZone, isFitMode
  VideoClip          trim, speed, volume, isMuted, filter, rotation (clockwise), flip,
                     color correction: exposure, brightness, contrast, temperature, tint, saturation, hue (-1..1)
  AudioTrack         uri, timelineStartOffsetMs, trimStart/End, volume, isLooping, isVoiceover
  TextOverlay        text, style, size, colors, posX/posY (= CENTER, normalized), start/end ms, isSticker
  ExportPreset       resolution class, fps, bitrates; outputSizeFor(aspectRatio)
data/
  ProjectRepository  JSON file storage (atomic, mutex-serialized); ProjectJson = defensive decoder
  ProjectMediaStore  per-project media folder: filesDir/projects/<id>/media
  BuiltInAudioHelper, VoiceRecorderHelper, SampleMediaHelper (synthesized SFX, mic, demo clip)
engine/
  VideoPlayerManager   ExoPlayer playlist of clips + one ExoPlayer per audio track, kept in sync
  VideoExportManager   Builds a Media3 Composition and exports to MediaStore Movies/Videora
  ClipColorMatrix      SINGLE source of truth for clip color (filter + correction)
  TextOverlayRenderer  OverlayMetrics (shared sizing) + BitmapOverlay that draws text for export
ui/
  screens/EditorScreen    wires everything; snackbar (undo), BackHandler, sheets
  screens/HomeScreen      project list, rename/delete dialogs, thumbnails
  viewmodel/EditorViewModel  all edit actions, undo/redo (25-deep project snapshots), selection
  components/             TimelineView, ClipActionRow (contextual toolbar), VideoPlayerView,
                          TextOverlayCanvas, *Sheet bottom sheets
```

### Key design decisions (keep these unless you have a reason to change them)

1. **The preview must match the export.** Each feature is implemented in both places:
   - **Color:** `ClipColorMatrix.matrixFor(clip)` returns one 4x5 matrix. The preview applies it via
     `TextureView.setLayerPaint(ColorMatrixColorFilter)`, and the export converts it with
     `toMedia3Matrix()` into a Media3 `RgbMatrix`. To add a color control, add it to the matrix
     and both sides get it.
   - **Text:** `OverlayMetrics` sizes text relative to the frame's short side (reference 240 units),
     in both `TextOverlayCanvas` (preview) and `TextOverlayBitmapOverlay` (export).
     `posX`/`posY` are the overlay's **center**.
   - **Mirror/rotation:** view transforms on the TextureView in the preview, `ScaleAndRotateTransformation`
     in the export. Rotation is stored clockwise; Media3 rotates counter-clockwise, so the export negates it.
   - **Speed:** `SpeedChangeEffect` + `SonicAudioProcessor` in the export, `PlaybackParameters` in the preview.
   - **Fit/Fill:** `Project.isFitMode`. The preview uses the PlayerView resize mode; the export uses the
     `Presentation` layout.
   - The preview deliberately does **not** use `ExoPlayer.setVideoEffects`. View-level transforms
     were chosen as lower risk without a device to test on.

2. **Export composition** (`VideoExportManager.buildComposition`):
   - Sequence 0 is the video clips. Each extra sequence is one audio track: an optional
     `addGap(offset)` followed by clipped items, repeated if looping, cut at the project end.
   - Text and stickers are a composition-level `OverlayEffect`, so their timestamps are on the
     output timeline after speed changes.
   - `experimentalSetForceAudioTrack(true)` creates silence for clips with no audio.
   - Clip/track volume uses a `ChannelMixingAudioProcessor` with diagonal matrices for 1–8 channels.
   - Encoder bitrates come from the preset, and `FrameDropEffect` caps output at the preset fps.

3. **Timeline** (`TimelineView`):
   - The playhead is fixed in the center, so **scroll offset = time**.
   - Widths are exactly proportional: `dpPerSecond`, default 48, pinch-zoom range 12–240.
     Never add minimum widths or spacing between clips; that breaks the time ↔ pixel mapping.
   - User scrubbing is detected through `scrollState.interactionSource` (drag + fling).
     Programmatic scrolls follow the playhead and never seek.
   - Pinch-zoom uses `PointerEventPass.Initial` and consumes only multi-touch events.

4. **Selection model** (`EditorUiState`): at most one of `selectedClipIndex` (-1 = none),
   `selectedOverlayId` or `selectedAudioTrackId` is set. `ClipActionRow` shows project, clip,
   overlay or audio tools based on it. Clip actions guard with `index in clips.indices`.

5. **Undo:** `pushUndoState(project)` before every mutation. For slider gestures, update live
   without undo and record one undo step at the end (see `updateSelectedClipColor` /
   `commitClipAdjustments`, `VolumeSheet`'s `onValueChangeFinished`).

6. **Storage safety:**
   - Saves write a temp file, then do an atomic move, under a shared `Mutex`.
   - A projects file that can't be parsed is renamed to `videora_projects.corrupt-<ts>.json`;
     it is never overwritten.
   - `ProjectJson.withDefaults` fills missing, null or unknown-enum fields from default instances.
     Gson bypasses Kotlin constructors, so without this a missing field becomes `null` in a
     non-null property and `copy()` crashes. **When you add a model field, give it a default,
     and add an enum field to `validateEnum` if it's an enum.**
   - All media a project uses lives in `filesDir/projects/<id>/`. Media in the cache and
     `content://` URIs get copied in (`ProjectMediaStore.adoptFile` / `localizeMedia`).
     Duplicating a project copies the folder; deleting a project deletes it.

### Conventions
- Match the existing style: Compose with explicit theme colors from `ui/theme/Color.kt`
  (`GoldChampagne`, `AccentCyan`, `NeonCoral`, `DarkSurface`...), short intent comments.
- Import order: all others, then `java.*`, `javax.*`, `kotlin.*`, each alphabetical.
- Commits: conventional style, e.g. `feat(editor): ...` or `fix(layout): ...`. Branch off `main` for work;
  the owner merges.
- The repo has release APKs (`videora-v*.apk`) in the root. Don't touch them.

## 4. Unverified on a device (check these first if bugs come in)

The last several rounds of work were built and unit-tested but **never run**. These are the
areas most likely to fail:
- **Export:** an audio track starting mid-video (`addGap` before the first item); mixing several
  audio sequences; the composition-level `OverlayEffect` timestamps; the 60 fps preset with
  `FrameDropEffect`; the custom `RgbMatrix` effect.
- **Color space:** Media3 may apply `RgbMatrix` in a different working color space than the
  on-screen `ColorMatrix`. Strong grades could look stronger or weaker in the export than in the preview.
- **Preview audio sync:** the secondary ExoPlayers re-seek when drift exceeds 150 ms.
  Watch for stutter at clip transitions.
- **Preview rotation:** 90°/270° rescaling of the TextureView in Fit vs Fill mode.
- **Timeline:** scrub detection via the interaction source; pinch-zoom alongside one-finger scrolling.
- **Text position change:** positions changed from top-left to center semantics, so text the user
  dragged in older projects shifts by half its size.
- **Noir B&W** now uses the luma-based grayscale from the shared matrix, so it looks slightly
  different from older exports.

## 5. Prioritized backlog

High value:
1. **Instrumented or screenshot tests.** There are none. Roborazzi/Robolectric would let an agent
   actually see the UI. Adding them needs the owner's OK, because they bring new test dependencies.
2. **Transitions between clips** (crossfade / slide). Not possible inside a single
   `EditedMediaItemSequence` in Media3 1.5.1. Needs a second video sequence and compositor, or a
   Media3 upgrade.
3. **Photo/image clips** (slideshows). Transformer supports images (`setDurationUs` + `setFrameRate`).
   The preview needs ExoPlayer image support or a placeholder.
4. **Auto-captions** from on-device speech recognition into `TextOverlay`s.
5. **Audio fades / ducking** under voiceover (needs a time-varying gain audio processor in the
   export and volume ramps in the preview).
6. **Background export** through a foreground service. Currently, leaving the editor can stop the
   export, since it runs in `viewModelScope`.

Medium:
- Trimming audio tracks and text directly on the timeline (dragging chip edges), and dragging
  chips to move them in time.
- Pinch/rotate text and stickers on the canvas (sizes are only set through the sheet today).
- Highlights/shadows/vibrance. These are non-linear, so they don't fit the matrix. Options are a
  3D LUT (`SingleColorLut`) for the export, plus a matching preview path.
- A blurred background for Fit mode instead of black bars.
- Clean up orphaned clip files (kept on purpose so undo works; never deleted until the project is).
- Split the large files: `EditorViewModel` (~840 lines), `HomeScreen` (~965), `TimelineView` (~720).
- Update the README and bump `versionName`/`versionCode` for this round of features.

Known smaller issues:
- `AudioSheet` still treats `audioTracks.first()` as "the" track, and its Remove clears **all** tracks.
  Per-track tools now live in the timeline's audio toolbar, so this sheet should be reconciled with them.
- Repeated splits keep adding to clip names ("(Part 1) (Part 2)...").
- `HomeViewModel.createDemoProject` uses fully-qualified names inline. Tidy it if you touch it.
- Clip and audio volume max out at 100% in the preview (ExoPlayer limit), while `AudioSheet`'s
  slider goes to 150%. The export honours >1, so the preview and export disagree above 100%.

## 6. Gotchas

- **Media3 1.5.1 API limits:** gaps are not allowed in video sequences; `EditedMediaItem.Builder.setSpeed`
  doesn't exist (use the effect + Sonic). `ExoPlayer.volume` is capped at 1.0.
- `Transformer` reports progress by polling (`getProgress`). Export state lives in `VideoExportManager.exportState`.
- `VideoPlayerManager.playerState.currentPositionMs` is **timeline** time (speed-adjusted, summed
  over clips), not the ExoPlayer position. Use `seekToTimeline()`, never `exoPlayer.seekTo` directly.
- Calling `setClips()` re-prepares the player. Don't call it for color-only changes; the preview
  look updates on its own from `activeClip`.
- `TextOverlay` styles are an enum (`TextStylePreset`). Renaming entries breaks saved projects
  unless `validateEnum` falls back.
