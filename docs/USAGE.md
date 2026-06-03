# Usage

## Basic Workflow

SplicePoint's current workflow is:

```text
upload audio
→ review waveform
→ review suggested loop candidates
→ audition a region
→ drag or resize if needed
→ export selected WAV loop
```

## Start the Backend

Before using the app, start the backend:

```bash
./backend/mvnw spring-boot:run
```

Then open:

```text
frontend/index.html
```

## Upload Audio

Use the upload panel in the browser:

- drag an audio file into the drop zone, or
- click the drop zone and choose a file

For Phase 1 testing, use WAV or AIFF when possible.

## Review Analysis

After upload, the frontend sends the audio to the backend for analysis.

The analysis panel shows:

- file name
- duration
- sample rate
- channel count
- engine status
- warnings

Warnings are normal during the prototype stage. For example, BPM and beat-grid detection are not finished yet.

## Review Loop Candidates

SplicePoint shows candidate regions in two places:

- waveform overlays
- the loop candidate list

Each candidate has a label, time range, confidence value, and reasons.

Current candidate reasons may include:

- strong transient near start
- clean boundary estimate
- stable energy window
- phase-1 friendly loop length

## Audition a Loop

Click a waveform region or candidate card to audition it.

The selected loop appears in the selection readout:

```text
Selected: 0.000s → 2.000s (2.000s)
```

## Edit a Loop

Regions are draggable and resizable in the waveform.

Use this when a candidate is close but not quite right.

Manual adjustment is expected. User corrections will eventually become feedback for better prediction.

## Export a Loop

Click:

```text
Export Selected WAV
```

The frontend sends the selected start/end time to the backend. The backend returns a WAV file.

The current export includes a small default fade to reduce edge clicks.

## Current Limitations

- BPM detection is not finished.
- Beat-grid snapping is not finished.
- Candidate ranking is still heuristic.
- Feedback learning is planned but not implemented.
- Format support depends on browser support and Java Sound support.
- The backend currently exports one selected loop at a time.

## Recommended Test File

Use a short WAV file first.

Suggested first tests:

- 1 to 10 seconds
- WAV format
- clear transient material such as drums or plucked audio
- not a massive full-length file

Longer and codec-heavy files should be tested later after the core workflow is stable.
