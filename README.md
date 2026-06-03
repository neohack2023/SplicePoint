# SplicePoint

SplicePoint is a browser-based sample extractor for producers, DJs, remixers, and audio tinkerers who want fast, reliable loops without digging through waveforms manually.

## Current Phase 1 Goal

Upload audio, inspect candidate splice regions, audition loops, adjust the selected region, and export a usable WAV slice.

SplicePoint is not trying to become a full DAW. It is a focused loop utility: quick in, clean loop out.

## Features

- Upload browser-supported audio through the frontend.
- Render an interactive waveform with WaveSurfer.js.
- Display candidate loop regions returned by the backend.
- Drag and resize regions in the waveform.
- Audition a loop by clicking a region or candidate card.
- Export the selected region as a WAV file.
- Return Phase 1 analysis metadata: duration, sample rate, channel count, heuristic candidates, confidence scores, and warnings.

## Frontend

The static frontend lives in [`frontend/`](frontend/) and uses WaveSurfer.js with Regions and Timeline plugins.

When opened directly from disk, the frontend points API calls to `http://localhost:8080`. When served from another origin, it uses same-origin API paths.

Open this file in a browser for local Phase 1 testing:

```text
frontend/index.html
```

## Backend

The backend lives in [`backend/`](backend/). It is a Spring Boot application that exposes the Phase 1 audio API.

### Endpoints

#### `GET /api/health`

Returns backend status and engine identity.

#### `POST /api/extract`

Accepts multipart form data:

```text
file=<audio file>
```

Returns JSON with:

- file name
- content type
- byte size
- duration seconds
- sample rate
- channel count
- BPM estimate placeholder fields
- engine status
- loop candidates
- warning messages

Loop candidates include:

- `start`
- `end`
- `duration`
- `confidence`
- `label`
- `reasons`

#### `POST /api/export`

Accepts multipart form data:

```text
file=<audio file>
start=<seconds>
end=<seconds>
fadeMs=<optional fade length, default 5>
```

Returns a downloadable `audio/wav` response.

## Run Backend

From the repo root:

```bash
./backend/mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\backend\mvnw.cmd spring-boot:run
```

The backend defaults to:

```text
http://localhost:8080
```

## Test Backend

```bash
./backend/mvnw test
```

The first run needs network access so Maven can resolve Spring Boot dependencies.

## Phase 1 Format Note

The backend currently uses Java Sound for decoding and WAV export. WAV and AIFF are the safest local test formats. Broader MP3 or codec-heavy support should be treated as a later backend-engine upgrade, likely through FFmpeg or another dedicated audio stack.

## Development Direction

The current implementation keeps the frontend and backend contract simple:

```text
upload file
→ /api/extract returns loop candidates
→ user selects or edits region
→ /api/export returns selected WAV slice
```

Future roadmap work should improve the analysis engine without changing that basic workflow unless absolutely necessary.
