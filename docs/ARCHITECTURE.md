# Architecture

## Overview

SplicePoint is currently a two-part prototype:

```text
browser frontend
→ Spring Boot backend API
→ Java Sound decoding / heuristic analysis / WAV export
```

The frontend owns the user workflow. The backend owns Phase 1 audio analysis and export.

## Major Components

| Component | Location | Responsibility |
|---|---|---|
| Frontend UI | `frontend/index.html` | Browser shell and visible app layout |
| Frontend controller | `frontend/app.js` | Upload handling, API calls, WaveSurfer setup, region selection, export flow |
| Frontend styles | `frontend/styles.css` | Visual layout and interaction styling |
| Backend API | `backend/src/main/java/com/example/backend/AudioController.java` | `/api/health`, `/api/extract`, `/api/export` endpoints |
| Audio services | `backend/src/main/java/com/example/backend/AudioServices.java` | Decode, analyze, score candidates, and encode selected WAV export |
| Backend tests | `backend/src/test/java/com/example/backend/BackendApplicationTests.java` | Synthetic WAV regression tests |

## Runtime Flow

```text
1. User opens frontend/index.html
2. User uploads an audio file
3. Frontend loads waveform with WaveSurfer.js
4. Frontend posts the file to POST /api/extract
5. Backend decodes audio through Java Sound
6. Backend produces candidate loop regions
7. Frontend draws editable regions and candidate cards
8. User auditions or edits a region
9. Frontend posts file + start/end to POST /api/export
10. Backend returns a WAV blob for download
```

## API Boundary

### `GET /api/health`

Returns backend service status and engine identity.

### `POST /api/extract`

Input:

```text
multipart/form-data
file=<audio file>
```

Output includes:

- file metadata
- duration
- sample rate
- channel count
- BPM placeholder fields
- engine status
- loop candidates
- warnings

### `POST /api/export`

Input:

```text
multipart/form-data
file=<audio file>
start=<seconds>
end=<seconds>
fadeMs=<optional milliseconds, default 5>
```

Output:

```text
audio/wav
```

## Current Analysis Engine

The Phase 1 backend currently performs:

- audio decoding through Java Sound
- conversion to PCM-style sample buffers
- mono analysis buffer creation
- energy window calculation
- transient-style scoring
- boundary cleanliness estimates
- heuristic loop candidate generation
- WAV encoding for selected export

It does **not** yet provide full BPM detection, beat-grid alignment, self-similarity analysis, feedback learning, or SLM-based ranking.

## Data Flow

```text
MultipartFile
→ AudioDecoder
→ DecodedAudio
→ mono buffer
→ energy windows
→ loop candidates
→ AnalysisResponse
```

For export:

```text
MultipartFile + start/end/fadeMs
→ AudioDecoder
→ frame selection
→ optional fade
→ WAV encoder
→ audio/wav response
```

## Dependencies

Backend:

- Java 21
- Spring Boot 3.5.5
- Maven Wrapper
- Java Sound API

Frontend:

- browser JavaScript
- WaveSurfer.js loaded from CDN
- WaveSurfer Regions plugin
- WaveSurfer Timeline plugin

## Extension Points

Future work should keep these boundaries stable where possible:

| Extension Point | Future Use |
|---|---|
| analysis service | BPM, beat grid, onset detection, self-similarity, structure detection |
| loop scoring | score components, learned ranking, user preference weighting |
| feedback endpoint | good/bad ratings, preview/export logs, edit-distance learning |
| export service | multi-loop export, naming, normalization, format options |
| frontend candidate renderer | score breakdowns, beat-grid display, feedback controls |

## Known Architecture TODOs

- Rename Java package from `com.example.backend` to a SplicePoint-specific package.
- Split `AudioServices.java` into smaller packages as the analysis engine grows.
- Add explicit API schema documentation once endpoints stabilize.
- Replace permissive development CORS before any hosted deployment.
- Decide whether Phase 2 analysis remains Java-only or adds an isolated Python research harness.
