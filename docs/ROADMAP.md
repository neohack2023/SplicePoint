# SplicePoint Roadmap

## Current Status

SplicePoint is an active prototype.

The Phase 1 frontend/backend workflow has been merged into `main`:

```text
upload audio
→ backend extracts Phase 1 candidates
→ frontend displays editable regions
→ user selects or adjusts a loop
→ backend exports selected WAV slice
```

The current system is useful for early testing, but it is not yet a full musical-analysis engine.

## Product Goal

SplicePoint should become a fast browser-based loop extraction tool for producers, DJs, remixers, and audio tinkerers.

Core promise:

> Upload a track, detect useful musical splice points, preview clean loop regions, adjust them if needed, and export usable WAV loops with minimal friction.

SplicePoint should stay focused. It is not trying to become a full DAW.

## Guiding Principles

- Keep the workflow simple: upload, analyze, preview, adjust, export.
- Prefer musical usefulness over feature bloat.
- Make suggested loops explainable with confidence indicators.
- Keep manual override available so the user stays in control.
- Treat user edits as learning signal, not failure.
- Keep frontend and backend contracts clear.
- Avoid heavy dependencies until the core loop workflow is stable.

---

# Active Work

## Phase 1: Stabilize Prototype

### Status

Active.

The basic backend/frontend pipeline exists. The next work is hardening and cleanup.

### Completed

- Spring Boot backend scaffold.
- `GET /api/health`.
- `POST /api/extract`.
- `POST /api/export`.
- Frontend upload and waveform flow.
- Editable loop regions.
- Candidate cards.
- Selected-region WAV export.
- Basic backend regression tests with synthetic WAV.
- Initial project documentation structure.

### Remaining

- Fix any WaveSurfer v7 compatibility issues from CDN plugin usage.
- Rename Java package from `com.example.backend` to a SplicePoint-specific package.
- Split large `AudioServices.java` into smaller analysis/export modules.
- Add controller/API tests for invalid inputs.
- Add CI workflow for backend tests.
- Decide whether to add a root `LICENSE` file.

### Definition of Done

- User can run backend locally.
- User can open frontend locally.
- User can upload a short WAV-compatible audio file.
- User can see candidate regions.
- User can adjust a selected region.
- User can export a WAV loop.
- Basic tests pass.
- Setup, usage, testing, and troubleshooting docs are current.

---

# Planned Work

## Phase 2: Real Loop Detection

### Goal

Replace Phase 1 heuristic loop suggestions with stronger musical analysis.

### Planned Features

- onset/transient detection
- RMS and energy-window features
- zero-crossing or low-amplitude boundary cleanup
- seam-risk scoring
- score components per candidate
- stable `analysisRunId`
- stable `candidateId`

### Definition of Done

- `/api/extract` returns candidate IDs and score components.
- Candidate confidence comes from documented feature scores.
- Frontend can display top reasons without clutter.
- Export behavior remains stable.

## Phase 3: Tempo and Beat Grid

### Goal

Make loop candidates musically timed, not just clean-looking.

### Planned Features

- global BPM estimate
- BPM confidence
- beat timestamps
- beat-aligned loop candidates
- candidate `startBeatIndex`, `endBeatIndex`, and `loopBeats`
- manual BPM/grid correction later

### Definition of Done

- Synthetic click-track tests can verify BPM behavior.
- Low-confidence BPM falls back safely.
- Candidate ranking improves when beat alignment and boundary cleanliness agree.

## Phase 4: Feedback Learning

### Goal

Allow SplicePoint to learn from user choices over time.

### Planned Features

- `POST /api/feedback`
- JSONL feedback event storage for local prototype work
- preview/export/rating event hooks
- good/bad explicit ratings
- edit-distance tracking
- training dataset builder

### Definition of Done

- Feedback events are logged without raw audio by default.
- Candidate features and user outcomes can be joined for ranking experiments.
- Feedback failures do not break playback or export.

## Phase 5: Musical Structure and SLM Layer

### Goal

Use structural analysis and SLM-style modeling to improve candidate prediction.

### Planned Features

- self-similarity or matrix-profile experiments
- repetition score
- homogeneity score
- novelty boundary score
- statistical SLM short-term model for local expectation scoring
- optional small neural ranker after feedback data exists

### Definition of Done

- Structure scores are exposed as candidate score components.
- Statistical expectation scoring improves candidate ranking in repeated-pattern tests.
- Heuristic fallback remains available.

## Phase 6: Export Improvements

### Goal

Make exported loops more useful in real production workflows.

### Planned Features

- better filename generation
- configurable fade in/out
- optional normalization
- mono/stereo preservation checks
- multi-loop export
- future export metadata

### Definition of Done

- Exported loops are named clearly.
- Exported loops avoid obvious clicks at boundaries when possible.
- Users can export one or more usable loops.

## Phase 7: UX Polish

### Goal

Make the app feel intentional, fast, and creator-friendly.

### Planned Features

- clearer loading states
- unsupported-format messages
- better candidate score display
- beat-grid visualization when available
- responsive layout polish
- keyboard shortcuts

### Definition of Done

- A new user understands the workflow without reading the code.
- Errors are visible and useful.
- The interface feels like a focused audio tool, not a test page.

---

# Parked Ideas

These may be useful later, but should not distract from the core loop pipeline now.

- Browser-only analysis engine.
- Python research backend.
- FFmpeg-based decoding/export service.
- ONNX browser ranker.
- User accounts or cloud loop libraries.
- Audio library search.
- Stem separation.
- MIDI companion-loop generation.
- DAW plugin integration.

---

# Rejected For Now

These are intentionally out of scope at the current stage.

- Full DAW editing timeline.
- Cloud storage of uploaded raw audio by default.
- Large neural model as the first analysis engine.
- Automatic copyright/authenticity claims.
- Hidden feedback logging without explicit documentation.

---

# Near-Term Priority Stack

1. Verify frontend plugin compatibility and selected-region export in-browser.
2. Add stable `analysisRunId` and `candidateId`.
3. Add score component object to candidates.
4. Add controller tests and invalid-input tests.
5. Refactor backend analysis services into smaller modules.
6. Add feedback event schema and logging.
7. Add BPM/onset synthetic test fixtures.
8. Add CI for backend tests.
9. Continue Phase 2 musical-analysis work.

## Working Note

The first real milestone remains:

> Upload audio → get believable loop candidates → audition one → adjust it → export a clean WAV.

Everything else should make that loop smarter, safer, or easier to trust.
