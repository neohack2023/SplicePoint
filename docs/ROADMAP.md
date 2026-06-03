# SplicePoint Roadmap

## Current State Review

SplicePoint is currently an early-stage browser-based sample extraction tool.

The `main` branch contains a static WaveSurfer.js frontend with:

- Drag-and-drop or click-to-upload audio input
- Waveform rendering
- Timeline display
- Play, pause, stop, and export controls
- Basic loop-region overlays
- A frontend export request pointed at `/api/export`

The current frontend proves the interface direction, but the analysis layer is still placeholder-level. Loop suggestions are based on simple duration math rather than real musical analysis.

An open backend pull request exists that adds a Spring Boot service with:

- `POST /api/extract`
- `POST /api/export`
- Placeholder BPM output
- Duration extraction
- Simple loop suggestions
- WAV trimming between `start` and `end` seconds

That backend PR moves the project closer to a complete prototype, but the core SplicePoint identity still depends on replacing placeholder analysis with actual loop intelligence.

## Product Goal

SplicePoint should become a fast browser-based loop extraction tool for producers, DJs, remixers, and audio tinkerers.

The core promise:

> Upload a track, detect useful musical splice points, preview clean loop regions, and export usable WAV loops with minimal friction.

The goal is not to become a full DAW. The goal is to become a sharp, focused sample-slicing utility that gets creators from audio file to usable loop quickly.

## Guiding Principles

- Keep the workflow simple: upload, analyze, preview, export.
- Prefer musical usefulness over feature bloat.
- Make suggested loops explainable with confidence indicators.
- Keep manual override available so the user stays in control.
- Build the analysis system in stages instead of pretending placeholder math is finished intelligence.
- Keep frontend and backend contracts clear.
- Avoid heavy dependencies until the core loop workflow is proven.

## Phase 1: Stabilize Prototype

### Goals

- Merge or finish the backend PR once it builds cleanly.
- Ensure the frontend and backend agree on request and response formats.
- Make the export button work reliably against the backend.
- Document local run steps clearly.

### Tasks

- Merge backend scaffold after verification.
- Confirm `/api/extract` accepts uploaded audio and returns JSON.
- Confirm `/api/export` accepts `file`, `start`, and `end` fields.
- Update README with separate frontend and backend startup instructions.
- Add basic error messages in the frontend when upload, analysis, or export fails.
- Replace generic backend package naming later, such as `com.example.backend`, with a SplicePoint-specific package.

### Definition of Done

- User can run backend locally.
- User can open frontend locally.
- User can upload a WAV-compatible audio file.
- User can see suggested regions.
- User can export at least one WAV loop.

## Phase 2: Real Loop Detection

### Goals

Replace placeholder loop suggestions with actual musical analysis.

### First Analysis Targets

- Duration
- Sample rate
- Channel count
- Basic peak/transient detection
- Rough tempo estimate
- Candidate loop windows
- Zero-crossing cleanup near loop boundaries

### Candidate Loop Logic

Initial loop suggestions should prefer:

- Strong transient starts
- Bar-like durations
- Stable energy sections
- Clean loop endpoints
- Low click/pop risk
- Repeated rhythmic structure

### Confidence Score Inputs

A loop confidence score can be built from:

- Boundary cleanliness
- Transient strength near start point
- Energy consistency across the region
- Estimated bar alignment
- Repetition similarity
- Click/pop risk

### Definition of Done

- `/api/extract` returns loop candidates based on audio features, not hardcoded duration slices.
- Each loop candidate includes start, end, duration, and confidence.
- Frontend displays multiple candidates clearly.

## Phase 3: Interactive Editing

### Goals

Make the frontend feel like a real loop workstation instead of a static preview.

### Tasks

- Allow regions to be dragged and resized.
- Add selected-region state.
- Export the currently selected region instead of always using the first region.
- Add visible start/end/duration readout.
- Add snap-to-beat or snap-to-grid once beat grid exists.
- Add keyboard shortcuts for play, stop, loop, and export.

### Definition of Done

- User can accept a suggested loop or manually adjust it.
- Export uses the adjusted user selection.
- The UI clearly shows what will be exported.

## Phase 4: Beat Grid and BPM Intelligence

### Goals

Build the musical timing layer that makes SplicePoint useful for producers.

### Tasks

- Add BPM estimation.
- Add beat-grid generation.
- Add bar-length options such as 1, 2, 4, 8, and 16 bars.
- Add confidence for BPM detection.
- Allow manual BPM override.
- Allow manual grid offset correction.

### Definition of Done

- SplicePoint can suggest musically timed loops.
- User can correct BPM/grid if the analysis guesses wrong.
- Loop candidates can be ranked by musical fit.

## Phase 5: Export Improvements

### Goals

Make exported loops more useful in real music workflows.

### Tasks

- Add export filename based on source track and time range.
- Add optional short fade in/out to reduce clicks.
- Add normalize option.
- Add mono/stereo preservation checks.
- Add export metadata where useful.
- Support exporting multiple selected loops.

### Definition of Done

- Exported files are named clearly.
- Exported loops avoid obvious clicks at boundaries.
- Users can export one or multiple usable loops.

## Phase 6: UX Polish

### Goals

Make the app feel intentional, fast, and creator-friendly.

### Tasks

- Add loading states for waveform and analysis.
- Add failed-file handling.
- Add unsupported-format messaging.
- Add candidate loop cards with confidence scores.
- Add lightweight visual styling beyond the current minimal CSS.
- Add mobile/tablet layout only if desktop flow is stable first.

### Definition of Done

- A new user understands the workflow without reading the code.
- Errors are visible and useful.
- The interface feels like a focused audio tool, not a test page.

## Phase 7: Testing and Project Hygiene

### Goals

Prevent future changes from breaking the core upload/analyze/export loop.

### Tasks

- Add backend unit tests for extraction and export.
- Add small test WAV fixtures.
- Add frontend lint or syntax checks.
- Add GitHub Actions CI for backend tests.
- Add a minimal contribution/dev guide.
- Add API contract documentation under `docs/`.

### Definition of Done

- CI can verify backend builds.
- Export behavior has regression coverage.
- API request/response shape is documented.

## Suggested Docs Structure

Future docs can live here:

```text
/docs
  ROADMAP.md
  API.md
  ANALYSIS_NOTES.md
  LOCAL_DEVELOPMENT.md
  LOOP_DETECTION_RESEARCH.md
```

## Near-Term Priority Stack

1. Finish backend PR and make export work end-to-end.
2. Update README with real run instructions.
3. Make frontend regions editable and export selected region.
4. Replace placeholder BPM and loop suggestions with first-pass analysis.
5. Add confidence scores.
6. Add beat-grid and manual correction.
7. Add tests and CI.

## Working Notes

The current version has a strong concept and a clean starting shape. The most important thing is to avoid feature sprawl before the core loop pipeline works.

The first real milestone should be:

> Upload audio → get believable loop candidates → audition one → adjust it → export a clean WAV.

Once that loop works, everything else becomes an upgrade instead of scaffolding wearing a trench coat.
