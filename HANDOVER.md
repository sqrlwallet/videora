# Videora: Agent Handover

Context for an agent continuing work on this repo. Read this before changing code.
Last updated 2026-10-22, on branch `feat/ai-phase1-openrouter-key` (off `main` after the v1.3.0
release; AI Phase 1, not merged or released yet).

## 1. What the app is

Videora is an offline, privacy-first Android video editor for vertical short-form video
(TikTok / Reels / Shorts). It has no accounts and no telemetry. The default **offline** edition
makes no network calls and has no INTERNET permission; the opt-in **ai** edition adds
bring-your-own-OpenRouter-key AI features (see decision 10). Kotlin +
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
| Version | `versionName 1.3.0`, `versionCode 8` (released 2026-10-22 as `videora-v1.3.0.apk`) |
| Build flavors | `offline` (default, no INTERNET) and `ai` (versionName suffix `-ai`, same applicationId) |
| Remote | `https://github.com/sqrlwallet/videora` |

## 2. Build and test

There is no `java` on PATH. Use Android Studio's bundled JDK:

```bash
export JAVA_HOME=/snap/android-studio/current/jbr
./gradlew :app:testOfflineDebugUnitTest :app:testAiDebugUnitTest
./gradlew :app:assembleOfflineRelease :app:assembleOfflineDebug
./gradlew :app:assembleAiRelease :app:assembleAiDebug
```

- With flavors, the old unflavored tasks (`testDebugUnitTest`, `assembleRelease`) no longer exist;
  `assembleRelease` builds both editions. Building all four APKs in one Gradle call made the daemon
  run out of memory here, so build one edition per call.
- After a release build, confirm the offline APK has no INTERNET permission:
  `~/Android/Sdk/build-tools/36.0.0/aapt dump badging <apk> | grep INTERNET` (expect nothing).

- Unit tests: `app/src/test/.../VideoraUnitTest.kt`, `ColorCorrectionTest.kt` and `OpenRouterTest.kt`
  (35 tests, all passing in both flavors).
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
  AudioTrack         uri, timelineStartOffsetMs, trimStart/End, volume, isLooping, isVoiceover,
                     fadeInMs/fadeOutMs; timelineLengthMs() + fadeGainAt() = shared fade math
  TextOverlay        text, style, size, colors, posX/posY (= CENTER, normalized), start/end ms, isSticker
  ExportPreset       resolution class, fps, bitrates; outputSizeFor(aspectRatio)
data/
  ProjectRepository  JSON file storage (atomic, mutex-serialized); ProjectJson = defensive decoder
  ProjectMediaStore  per-project media folder: filesDir/projects/<id>/media
  BuiltInAudioHelper, VoiceRecorderHelper, SampleMediaHelper (synthesized SFX, mic, demo clip)
engine/
  VideoPlayerManager   ExoPlayer playlist of clips + one ExoPlayer per audio track, kept in sync
  VideoExportManager   Process-wide singleton (getInstance). Builds a Media3 Composition and exports
                       to MediaStore Movies/Videora
  FadeAudioProcessor   Per-segment volume + fade ramp for audio tracks in the export (PCM16/float)
  ExportService        Foreground service started with each export; mirrors exportState into a
                       progress notification (Cancel action) and a result notification, then stops
  ClipColorMatrix      SINGLE source of truth for clip color (filter + correction)
  TextOverlayRenderer  OverlayMetrics (shared sizing) + BitmapOverlay that draws text for export
ai/                     (package com.kevan.videorabykevan.ai, split across source sets)
  src/main     OpenRouter.kt  AiTask, AiKeyInfo, AiModel, AiError, AiClient interface,
                              OpenRouterJson (parsing + HTTP error mapping), AiKeyFormat. Pure Kotlin.
  src/offline  AiEdition      stub: isAvailable = false, client() = null, empty SettingsScreen
  src/ai       AiEdition      isAvailable = true, client() = OpenRouterClient when a key is saved
               ApiKeyStore    Keystore AES-GCM encrypted key + per-task model IDs in prefs "videora_ai"
               OpenRouterClient  HttpURLConnection (no new deps); GET /key, GET /models
               AiSettingsScreen / AiSettingsViewModel  key paste/check/remove, model picker
               AndroidManifest.xml  adds INTERNET (merged only into the ai edition)
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

7. **Background export:** `VideoExportManager` is one per process with its own
   `SupervisorJob` scope, so it outlives the `EditorViewModel`. `exportProject` ignores a call while
   an export is running. After `transformer.start` it starts `ExportService`, which only reflects
   state; it never drives the export. FGS type is `mediaProcessing` on API 35+ and `dataSync` on 29–34
   (both declared in the manifest). `POST_NOTIFICATIONS` is requested when an export starts, and the
   export runs whether or not it's granted.

8. **Volume is capped at 100% everywhere.** ExoPlayer can't play louder, so the export's
   `volumeProcessor` clamps to 1 as well, which also tames old projects saved with the former 150% slider.

9. **Audio fades** follow the "preview must match export" rule through `AudioTrack.fadeGainAt`.
   The export uses `FadeAudioProcessor` on each segment of the track's sequence (a looping track
   has several, so each gets its start offset). The preview sets the track player's volume from the
   same function on every sync tick (~33 ms). Fades are relative to the track's span on the timeline
   (`timelineLengthMs`): its own end, or the video's end for looping tracks.
   Music and SFX now get their length measured when added (`ProjectMediaStore.measureAudioTracks`),
   and older tracks without a length are measured on load. Before this, a looping music track with
   no length only played once in the export.

10. **Two editions (build flavors).** The `offline` edition must never gain network code or the
   INTERNET permission. Shared code talks to AI only through `AiEdition` (same members in
   `src/offline` and `src/ai`) and the `AiClient` interface in `src/main`. Put anything that does
   I/O or needs a network in `src/ai`; keep prompt building and response parsing in `src/main` as
   pure Kotlin so it's unit tested in both flavors. Rules for AI features:
   - Nothing is sent without an explicit tap on an AI action. A feature's first use should say what
     will be uploaded (Phase 2+).
   - AI results become ordinary edits: one `pushUndoState` per applied result, never auto-applied.
   - Model IDs are user-configurable per `AiTask`. The default is the alias
     `~google/gemini-flash-latest` (takes text + audio), so it doesn't go stale.
   - Parse JSON with `JsonParser`, not Gson reflection, so no new R8 keep rules are needed.
     (`AiSettingsViewModel` has a keep rule because the ViewModel factory creates it reflectively.)
   - The key is excluded from backups (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`).

### Releasing
Build each edition's release (see section 2), then copy
`app/build/outputs/apk/offline/release/app-offline-release.apk` to `videora-v<version>.apk` and
`app/build/outputs/apk/ai/release/app-ai-release.apk` to `videora-ai-v<version>.apk` in the root
(`*.apk` is gitignored). Add a row to the README download table, tag `v<version>` on `main`, then
`gh release create` with both APKs attached. Release builds are signed with the debug key (see
`app/build.gradle.kts`), the same as all earlier releases. Both editions share the applicationId,
so installing one over the other keeps the user's projects.

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
- **Background export (new):** the foreground service start, notification updates, the Cancel
  action, and the export surviving the app being backgrounded. Also check that tapping the
  notification brings the app back without restarting the editor.
- **Audio fades (new):** listen in the preview and in an export, especially a looping track's
  fade-out and a track that starts mid-video. A 16-bit vs float PCM mix-up would sound like loud noise.
- **Home export banner (new):** appears on Home while exporting; its Cancel stops the export.
- **Audio sheet (new):** now lists every track; tapping a row closes the sheet and selects that
  track (its timeline audio toolbar should appear).
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
4. **AI features (ai edition), in phases.** Phase 1 is done on this branch: editions, key storage,
   key check, model picker. Next:
   - **Phase 2, hooks and post captions (text only):** add `chat()` to `AiClient`
     (POST `/api/v1/chat/completions`, `response_format` JSON); "Generate hooks" returns ~5 options
     that become a VIRAL_HOOK `TextOverlay`; a post caption + hashtags with a copy button after export.
   - **Phase 3, auto-captions:** extract project audio (audio-only Transformer export), chunk long
     videos, send to POST `/api/v1/audio/transcriptions` (fallback: `input_audio` in chat), map
     segment timestamps from source time to timeline time (trim, speed, muted clips) as pure, tested
     Kotlin, then add caption overlays. First prototype whether the chosen models return per-segment
     timestamps; captions can't be timed without them. Show an estimated cost before sending.
   - **Phase 4, frame-based suggestions:** a few frames to a vision model for suggested color
     settings (clamped to the existing -1..1 controls) or a cover frame.
5. ~~Audio fades~~ (done; decision 9). **Ducking** under voiceover is next: it can reuse
   `FadeAudioProcessor`'s approach, with gain computed from the voiceover tracks' spans.
6. ~~Background export~~ (done; decision 7). Home shows a progress banner. Another project's
   editor still opens the shared export dialog showing that progress, which is acceptable but could
   say which project it belongs to (`ExportProgressState.projectTitle` is available).

Medium:
- Draw fade ramps on the timeline's audio chips.
- Trimming audio tracks and text directly on the timeline (dragging chip edges), and dragging
  chips to move them in time.
- Pinch/rotate text and stickers on the canvas (sizes are only set through the sheet today).
- Highlights/shadows/vibrance. These are non-linear, so they don't fit the matrix. Options are a
  3D LUT (`SingleColorLut`) for the export, plus a matching preview path.
- A blurred background for Fit mode instead of black bars.
- Clean up orphaned clip files (kept on purpose so undo works; never deleted until the project is).
- Split the large files: `EditorViewModel` (~840 lines), `HomeScreen` (~965), `TimelineView` (~720).

Known smaller issues:
- `addVideoUris` copies picked clips to `projects/<id>/clips`, not `ProjectMediaStore`'s `media`
  folder. Both are inside the project folder so duplicate/delete work, but it's inconsistent.
- Volume boost above 100% would need a gain `AudioProcessor` in the preview's ExoPlayer
  (custom `RenderersFactory`), then the export clamp in `volumeProcessor` can be lifted.

Fixed in the 1.3.0 round: AudioSheet's first-track/remove-all behavior (now a per-track list),
the 150% preview/export volume mismatch, and split names piling up (`splitPartName`, unit tested).
Also tidied: `HomeViewModel.createDemoProject`'s fully-qualified names.

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
