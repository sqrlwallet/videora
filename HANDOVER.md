# Videora: Agent Handover

Context for an agent continuing work on this repo. Read this before changing code.
Last updated 2026-10-23, on `main`: v1.5.0 release (AI tools in the ai edition).

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
| Version | `versionName 1.5.0`, `versionCode 10` (released 2026-10-23 as `videora-v1.5.0.apk` + `videora-ai-v1.5.0.apk`) |
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
  TextOverlay        text, style, size, colors, posX/posY (= CENTER, normalized), start/end ms, isSticker,
                     animation (TextAnimation); animationFrameAt(ms) = shared animation math
  ExportPreset       resolution class, fps, bitrates; outputSizeFor(aspectRatio)
data/
  ProjectRepository  JSON file storage (atomic, mutex-serialized); ProjectJson = defensive decoder
  ProjectMediaStore  per-project media folder: filesDir/projects/<id>/media
  BuiltInAudioHelper, VoiceRecorderHelper, SampleMediaHelper (synthesized SFX, mic, demo clip)
  StillVideoEncoder  photo / freeze frame -> silent 10 s H.264 MP4 (MediaCodec surface + MediaMuxer)
  TextToSpeechHelper "Read aloud": device TTS engine -> WAV in the cache (engine is a system service)
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
  src/main     OpenRouter.kt  AiTask (TEXT, VISION, AUDIO), AiKeyInfo, AiModel, AiError, AiContent/AiMessage,
                              AiClient (keyInfo, models, modelFor, chat), OpenRouterJson (chat request
                              body, reply + error parsing), AiKeyFormat. Pure Kotlin.
               AiPrompts.kt   every tool's prompt + lenient JSON reply parsing, caption timing
                              (speechPieces, mapToTimeline, captionOverlays), story layout. Pure, tested.
               AiMedia.kt     on-device prep only: 512 px JPEG frames, decode audio to 16 kHz mono WAV
               (ui) AiToolsViewModel + AiToolsSheet  the tools; EditorViewModel applies results
  src/offline  AiEdition      stub: isAvailable = false, client() = null, empty SettingsScreen
  src/ai       AiEdition      isAvailable = true, client() = OpenRouterClient when a key is saved
               ApiKeyStore    Keystore AES-GCM encrypted key + per-task model IDs in prefs "videora_ai"
               OpenRouterClient  HttpURLConnection (no new deps); GET /key, GET /models,
                                 POST /chat/completions (120 s read timeout)
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

11. **Text animations** (Fade, Pop, Slide up, Typewriter) follow "preview must match export" through
   `TextOverlay.animationFrameAt`. The preview applies it with `graphicsLayer` (the *selected* overlay
   is drawn static so it can be dragged); the export's `TextOverlayBitmapOverlay` keeps its per-set cache
   for static frames and redraws one reused bitmap while anything is animating (Media3 1.5.1
   re-uploads when `Bitmap.generationId` changes; checked with javap).

12. **Photos and freeze frames are encoded to video on import** (`StillVideoEncoder`, 10 fps,
   `STILL_SOURCE_MS` = 10 s, shown 3 s / 2 s by default via the trim) and flagged `VideoClip.isPhoto`.
   This deliberately avoids ExoPlayer's image renderer: PlayerView 1.5.1 shows images in its own
   ImageView, which would miss the TextureView color/mirror/rotation look, and mixing image and video
   items in one Transformer sequence is untested here. A photo is just a silent clip to every engine.
   The encoder assigns output timestamps itself (one frame apart); `SampleMediaHelper` now does too.

13. **Voice effects and ducking** follow "preview must match export" too. `VoiceEffect.pitch` goes to
   `PlaybackParameters(speed, pitch)` in the preview (main player for clips, each track's player for
   tracks) and to `SonicAudioProcessor.setPitch` in the export (combined with the speed Sonic for clips).
   `Project.duckMusicUnderVoice` dips every non-voiceover track to `DUCK_LEVEL` with `DUCK_RAMP_MS`
   ramps around voiceover spans (`model/AudioDucking.kt`): the preview multiplies it into the track
   volume each sync tick, the export passes the spans to `FadeAudioProcessor`.

14. **AI tools** (ai edition; `AiToolsSheet`, opened from the project toolbar's "AI Tools", the clip
   toolbar's "AI Color" and the text toolbar's "AI Rewrite"): auto captions, hook ideas, text story,
   rewrite (punchier/shorter/fix/emojis/translate), translate all text, auto color, post caption +
   hashtags (copy), name project. Rules kept: each tool states what it sends and to which model
   before its button is tapped; results are listed and only applied on tap, as one undo step via
   EditorViewModel (`addHookText`, `addAiOverlays`, `setOverlayTexts`, `applyColorSuggestion`,
   `renameProject`, `setSelectedOverlayText`). Replies are requested as plain JSON in the prompt,
   not `response_format` (OpenRouter fails the request on models without structured outputs), and
   parsed leniently (`AiPrompts.extractJson`). Auto captions decode each clip's / voiceover's own
   audio (60 s pieces, 10 min cap, muted clips/photos/music skipped) to 16 kHz mono WAV, send it as
   `input_audio`, and map segment times to the timeline through trim and speed
   (`mapToTimeline`); they're flagged `TextOverlay.isAutoCaption` so a re-run replaces them.

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
- **Direct-manipulation editing (new):** dragging the selected clip's gold edges trims it
  (`VideoClip.trimmedByDrag`, live preview in `TimelineView`, saved on release); dragging a
  *selected* text or audio chip moves it in time, snapping to the playhead, cuts and 0:00 with a haptic tick;
  pinching anywhere on the video resizes the selected text/sticker; dragged text snaps to the
  vertical center line; tapping empty video deselects the overlay (it then no longer toggles play).
  Check the drags don't fight the timeline scroll, and that a clip tap no longer moves the playhead
  when it is already inside the clip (so Split cuts where the user aimed). Text moves are now undoable.
- **TikTok/Instagram parity round (new):** text animations in preview and export; "Read aloud"
  (text-to-speech into a voiceover track; needs the manifest `<queries>` for `TTS_SERVICE` on API 30+;
  the engine's WAV sample rate differs from other audio, so check the export mixes it cleanly);
  long-press a clip and drag to reorder (gold drop marker; no auto-scroll at the strip's edges);
  drag the edge handles of a selected text chip to change its timing; "Replace" keeps a clip's edits
  and timeline length.
- **Photos and freeze frames (new):** "Add Media" picks photos and videos; check EXIF rotation on
  both API < 28 (BitmapFactory + ExifInterface) and 28+ (ImageDecoder), the encoder on a real device
  (timing, 10 s length, no green/garbled frames), and that Freeze grabs the frame under the playhead
  the right way up (MediaMetadataRetriever applies the video's rotation) and keeps the clip's look.
- **Voice effects and ducking (new):** listen to a pitched clip at 2x speed (one Sonic does both) and
  a pitched voiceover in the export vs the preview; check ducking around a voiceover in both, and
  that the preview's ~33 ms volume steps don't sound zippery.
- **Reorder auto-scroll (new):** hold a lifted clip near the strip's edge; the timeline should scroll
  and the clip stay under the finger.
- **Demo clip encoder:** now assigns its own frame timestamps and drains until end of stream.
- **AI tools (new, ai edition):** none has been run against OpenRouter. Check with a real key:
  that `~google/gemini-flash-latest` accepts `input_audio` WAV and returns usable segment times
  (captions are only as good as these), image parts for Auto color / hooks with frames, the JSON
  parsing on real replies, error messages (no credits, rate limit), Cancel mid-request, and how long
  a 1-minute transcription takes against the 120 s read timeout. `AiMedia.decodeSpeechPcm` (MediaCodec)
  is also unverified: check AAC and Opus sources and a voiceover recording.
- **Noir B&W** now uses the luma-based grayscale from the shared matrix, so it looks slightly
  different from older exports.

## 5. Prioritized backlog

High value:
1. **Instrumented or screenshot tests.** There are none. Roborazzi/Robolectric would let an agent
   actually see the UI. Adding them needs the owner's OK, because they bring new test dependencies.
2. **Transitions between clips** (crossfade / slide). Not possible inside a single
   `EditedMediaItemSequence` in Media3 1.5.1. Needs a second video sequence and compositor, or a
   Media3 upgrade.
3. ~~Photo/image clips~~ (done; decision 12). Next for slideshows: an optional slow zoom (Ken Burns)
   per photo, which could be baked in at encode time.
4. **AI features (ai edition).** Phase 1 (v1.4.0): editions, key storage, key check, model picker.
   Phases 2–4 shipped in v1.5.0 (decision 14) but were **never run against OpenRouter**: no key or device here.
   Next: verify each tool live (see section 4), then a cost estimate before sending (needs the
   model's price, i.e. a `GET /models` call the user agrees to), a cover-frame picker, and
   word-by-word animated captions.
5. ~~Audio fades~~ (done; decision 9). ~~Ducking~~ (done; decision 13).
6. ~~Background export~~ (done; decision 7). Home shows a progress banner. Another project's
   editor still opens the shared export dialog showing that progress, which is acceptable but could
   say which project it belongs to (`ExportProgressState.projectTitle` is available).

Medium:
- Draw fade ramps on the timeline's audio chips.
- Trimming audio tracks on the timeline (dragging chip edges; text chips already have this).
- Rotating text and stickers on the canvas (pinch-to-resize is done).
- TikTok/Instagram features not yet here: reverse
  (needs a re-encode), non-pitch voice effects (robot, echo: need a custom AudioProcessor in the
  export and a preview RenderersFactory), clip opacity/overlay (picture-in-picture), keyframes,
  green screen/cutout (segmentation model), beat marks, auto-captions (backlog item 4, Phase 3).
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
