# SplicePoint Musical Analysis Roadmap

## Purpose

This roadmap translates the musical-analysis research into an implementation plan for SplicePoint.

The goal is to evolve SplicePoint from a basic waveform loop exporter into a real music-aware loop finder that can:

- detect meaningful splice points
- estimate tempo and beat structure
- suggest musically useful loop candidates
- score candidate quality
- learn from user feedback over time
- improve predictions without turning the app into a bloated DAW

This document is split into two tracks:

1. **Human Developer Roadmap**: architecture, milestones, implementation choices, testing, and product decisions.
2. **LLM Developer Roadmap**: task prompts, coding guardrails, review checklists, and implementation contracts for AI-assisted development.

The shared strategy is:

```text
classical DSP first
→ stable candidate schema
→ user feedback logging
→ supervised ranking model
→ optional ONNX/SLM re-ranking
→ backend only where it adds real value
```

## Core Product Direction

SplicePoint should treat loop detection as a music information retrieval problem, not just a waveform slicing problem.

A technically clean loop is not automatically a musically useful loop. A good loop candidate should usually satisfy several conditions:

- starts near a meaningful transient or musical boundary
- aligns with a beat, bar, or phrase grid
- has a plausible duration such as 1, 2, 4, 8, or 16 beats
- has low seam risk at the start/end boundary
- preserves rhythmic or harmonic continuity
- receives useful feedback from real user behavior

The first production-ready system should not try to find one perfect loop. It should generate multiple plausible loop candidates, rank them, let users adjust them, and learn from what users accept or reject.

---

# Shared Architecture

## Recommended Engine Shape

Use a modular analysis engine with swappable implementations.

```text
Decode
→ Normalize
→ Feature Extraction
→ Beat / Tempo Analysis
→ Candidate Generation
→ Candidate Scoring
→ Feedback Logging
→ Optional ML Re-ranking
```

Each stage should have clear input/output contracts so it can run in one of several places:

- browser worker
- Java backend
- future Python reference backend
- future ONNX/SLM ranker

## Why Modular Instead of Backend-Only

A backend is useful, but it should not own every decision by default.

Use the browser path when:

- files are short enough to decode locally
- format is browser-supported
- user privacy matters
- minimal server calls are preferred
- analysis does not require heavy models

Use the backend path when:

- files are large
- browser decode fails
- format conversion is needed
- native FFmpeg is required
- centralized model training is needed
- batch evaluation or dataset generation is needed

## Canonical Data Contracts

### `AnalysisRequest`

```json
{
  "file": "multipart audio file",
  "options": {
    "maxCandidates": 8,
    "preferredLoopBeats": [1, 2, 4, 8, 16],
    "analysisMode": "fast | balanced | deep",
    "enableModelRanking": false
  }
}
```

### `AnalysisResponse`

```json
{
  "fileName": "track.wav",
  "contentType": "audio/wav",
  "byteSize": 1234567,
  "durationSeconds": 42.5,
  "sampleRate": 44100,
  "channels": 2,
  "bpmEstimate": 128.0,
  "bpmConfidence": 0.82,
  "engineStatus": "phase-2-dsp",
  "beats": [],
  "downbeats": [],
  "loops": [],
  "warnings": []
}
```

### `LoopCandidate`

```json
{
  "candidateId": "loop_001",
  "start": 8.0,
  "end": 16.0,
  "duration": 8.0,
  "startBeatIndex": 32,
  "endBeatIndex": 64,
  "loopBeats": 32,
  "confidence": 0.88,
  "heuristicScore": 0.81,
  "modelScore": null,
  "label": "bar-aligned phrase candidate",
  "reasons": [
    "strong transient near start",
    "stable beat grid",
    "low seam risk"
  ],
  "features": {
    "startOnsetStrength": 0.91,
    "endBoundaryCleanliness": 0.72,
    "tempoStability": 0.86,
    "repetitionScore": 0.64,
    "seamRisk": 0.18
  }
}
```

### `FeedbackEvent`

```json
{
  "eventId": "evt_001",
  "analysisRunId": "run_001",
  "candidateId": "loop_001",
  "eventType": "previewed | accepted | rejected | exported | edited | favorited",
  "timestamp": "ISO-8601",
  "displayedRank": 1,
  "editedStart": 8.031,
  "editedEnd": 15.984,
  "previewDurationMs": 12000,
  "explicitRating": "good | bad | neutral | null",
  "engineVersion": "phase-2-dsp-v1"
}
```

---

# Human Developer Roadmap

## Phase 2A: DSP Foundation

### Goal

Replace placeholder duration-based loop suggestions with real musical feature extraction.

### Build

- Decode audio into normalized PCM.
- Create mono analysis buffer.
- Preserve original metadata for export/playback.
- Add onset envelope calculation.
- Add RMS/energy windows.
- Add transient scoring.
- Add seam-risk scoring.
- Keep current backend API stable where possible.

### First Algorithms

Use classical DSP first:

- spectral flux onset envelope
- energy/RMS envelope
- high-frequency content style transient score
- zero-crossing and local amplitude boundary checks
- basic autocorrelation or tempogram-style tempo estimate

### Output

`/api/extract` should return:

- duration
- sample rate
- channel count
- onset markers
- rough BPM estimate
- BPM confidence
- loop candidates with explainable reasons

### Definition of Done

- Loop candidates are no longer hardcoded duration slices.
- At least three candidate types exist:
  - opening slice
  - transient-start candidate
  - beat-length candidate
- Candidate confidence is based on feature scores, not magic numbers.
- Existing export workflow still works.

## Phase 2B: Beat and Tempo Layer

### Goal

Make loop candidates musically timed, not merely clean-looking.

### Build

- Estimate global BPM.
- Generate approximate beat grid.
- Detect beat phase.
- Generate candidates at beat-based lengths.
- Support 1, 2, 4, 8, and 16 beat windows.
- Add manual BPM override later.
- Add grid offset correction later.

### Candidate Rules

Prefer loop starts that are:

- near onset peaks
- close to beat-grid points
- consistent with estimated tempo
- not inside obvious silence unless intentional

Prefer loop ends that:

- land on a beat boundary
- produce low seam discontinuity
- preserve rhythmic length expectations

### Definition of Done

- `/api/extract` returns beat timestamps.
- Loop candidates include `startBeatIndex`, `endBeatIndex`, and `loopBeats`.
- User can see whether a loop is beat-aligned.
- Confidence improves when beat alignment and boundary cleanliness agree.

## Phase 2C: Candidate Scoring System

### Goal

Create a transparent ranking system that can later feed an SLM/ranker.

### Scoring Components

Each candidate should get component scores:

| Score | Meaning |
|---|---|
| `onsetScore` | strength of musical event near start |
| `beatAlignmentScore` | closeness to beat/bar grid |
| `tempoStabilityScore` | consistency of pulse inside the loop |
| `boundaryCleanlinessScore` | low click/pop risk near start/end |
| `seamRiskScore` | likelihood of audible discontinuity at loop seam |
| `repetitionScore` | internal rhythmic/spectral repeat quality |
| `durationScore` | usefulness of loop length |
| `structureScore` | optional section/boundary usefulness |

### Formula v1

Start simple and tunable:

```text
confidence =
  onsetScore * 0.20
+ beatAlignmentScore * 0.20
+ tempoStabilityScore * 0.15
+ boundaryCleanlinessScore * 0.15
+ (1 - seamRiskScore) * 0.15
+ repetitionScore * 0.10
+ durationScore * 0.05
```

Keep the weights in one location so they can be tuned or learned later.

### Definition of Done

- Candidate score is explainable.
- Frontend can display reasons.
- Score components are available for training logs.
- Changing weights does not require rewriting the pipeline.

## Phase 2D: Feedback Logging

### Goal

Collect useful training data before building the model.

The first learning system should not start with a neural model. It should start with clean logs.

### Log These Events

- candidate shown
- candidate previewed
- candidate skipped
- region edited
- candidate accepted
- candidate exported
- candidate explicitly rated good/bad
- candidate deleted/rejected

### Derived Labels

Use labels such as:

| Label | Rule |
|---|---|
| strong positive | exported or favorited |
| positive | accepted or previewed repeatedly |
| weak positive | previewed longer than threshold |
| neutral | shown but ignored |
| negative | rejected or skipped quickly |
| correction signal | user adjusted start/end significantly |

### Privacy Rule

By default, store features and interactions, not raw audio.

Only upload raw audio for model research if the user explicitly opts in later.

### Definition of Done

- Feedback events are stored in a stable JSONL or database format.
- Each event includes engine version and candidate features.
- We can reconstruct why a candidate was shown and how the user reacted.

## Phase 2E: First Learning Ranker

### Goal

Use feedback to improve candidate ranking over time.

### First Model

Start with a small supervised ranker over engineered features:

- logistic regression
- gradient boosted trees
- tiny MLP
- pairwise ranking model

Do not start with an end-to-end audio neural network.

### Input Features

- beat alignment error
- loop length in seconds
- loop length in beats
- onset strength near start
- onset strength near end
- RMS seam difference
- spectral seam difference
- tempo stability
- repetition score
- duration score
- candidate rank from heuristic system
- engine version
- file duration bucket
- content type

### Labels

Primary label:

```text
accepted/exported with little or no editing = positive
rejected/heavily edited/skipped quickly = negative
```

### Deployment

Train server-side first. Export to ONNX later if browser inference is useful.

### Definition of Done

- Model can re-rank candidate lists offline.
- Evaluation shows lift over heuristic-only ranking.
- Model output is exposed as `modelScore` while keeping `heuristicScore` visible.

## Phase 2F: Optional ONNX / Browser Ranker

### Goal

Run lightweight candidate re-ranking in the app without extra server calls.

### Build

- Export trained model to ONNX.
- Load with ONNX Runtime Web.
- Run inference on candidate feature vectors.
- Blend model score with heuristic score.

### Blended Score

```text
finalScore = heuristicScore * 0.70 + modelScore * 0.30
```

Increase model weight only after validation.

### Definition of Done

- App can rank candidates without contacting the server for every prediction.
- Model can be disabled with a feature flag.
- Heuristic fallback always works.

## Phase 2G: Python Research Harness

### Goal

Use Python for research and benchmarking without forcing Python into production too early.

### Build

Create a separate research area:

```text
/research
  datasets/
  notebooks/
  scripts/
  baselines/
  models/
```

### Use Python For

- librosa baseline comparison
- madmom beat/downbeat comparison
- evaluation datasets
- feature experiments
- model training
- ONNX export

### Definition of Done

- Java/browser/backend outputs can be compared against Python reference outputs.
- We have a small golden test set.
- We can measure improvements instead of guessing.

---

# LLM Developer Roadmap

## LLM Operating Rules

When an LLM modifies SplicePoint, it must obey these rules:

1. Keep `main` as the primary working branch unless told otherwise.
2. Do not introduce extra branches unless explicitly requested.
3. Preserve the existing API contracts unless the task is specifically to change them.
4. Do not replace the whole backend when a smaller service/module change works.
5. Do not hide placeholder math behind confident names.
6. Every scoring heuristic must expose reasons or component scores.
7. Every model/ranker feature must be logged with a version.
8. Raw audio should not be logged by default.
9. Prefer small commits with clear messages.
10. If unsure whether a dependency is acceptable, document the tradeoff instead of silently adding it.

## LLM Task Prompt: Add DSP Feature Extraction

Use this prompt for an LLM coding agent:

```text
Update SplicePoint's backend analysis service to add real DSP feature extraction.

Requirements:
- Preserve existing `/api/extract` and `/api/export` endpoints.
- Add an internal feature extraction layer that computes:
  - mono PCM analysis buffer
  - RMS energy windows
  - onset envelope using spectral-flux-style frame differences if possible
  - transient score per analysis frame
  - local boundary cleanliness near candidate start/end
- Return feature-derived loop candidates instead of hardcoded duration-only suggestions.
- Each candidate must include start, end, duration, confidence, label, reasons, and feature component scores.
- Keep implementation dependency-light unless a dependency is justified in docs.
- Add tests using synthetic WAV input.
- Do not change frontend behavior except where needed to display new fields.
```

### Review Checklist

- Does `/api/extract` still accept multipart `file`?
- Are existing frontend calls still valid?
- Are candidates explainable?
- Is confidence calculated from actual features?
- Are tests deterministic?
- Does export still return WAV?

## LLM Task Prompt: Add Tempo and Beat Grid

```text
Implement first-pass tempo and beat-grid estimation for SplicePoint.

Requirements:
- Build on the existing analysis service.
- Estimate global BPM from the onset envelope using autocorrelation or an equivalent periodicity method.
- Return `bpmEstimate` and `bpmConfidence`.
- Generate approximate beat timestamps.
- Generate loop candidates using beat-based lengths: 1, 2, 4, 8, and 16 beats when possible.
- Include beat indices in each candidate.
- Keep fallback behavior for files where BPM confidence is low.
- Add tests for synthetic click tracks at known BPM values.
```

### Review Checklist

- Does the estimated BPM land near known synthetic BPM tests?
- Are half-time/double-time mistakes documented or handled?
- Does low-confidence BPM still produce safe fallback candidates?
- Are beat timestamps returned in seconds?
- Does the frontend avoid crashing if beat data is empty?

## LLM Task Prompt: Add Candidate Score Components

```text
Refactor SplicePoint loop scoring into named score components.

Requirements:
- Add a `scoreComponents` object to each loop candidate.
- Include at least:
  - onsetScore
  - beatAlignmentScore
  - tempoStabilityScore
  - boundaryCleanlinessScore
  - seamRiskScore
  - repetitionScore
  - durationScore
- Compute final confidence from a documented weighted formula.
- Keep weights centralized in one class/module.
- Add tests that verify confidence changes when component scores change.
- Update frontend candidate display to show top reasons without clutter.
```

### Review Checklist

- Are score components stable JSON fields?
- Are weights easy to tune?
- Is the formula documented?
- Can the same fields be used later for ML training?

## LLM Task Prompt: Add Feedback Logging

```text
Add user feedback logging to SplicePoint.

Requirements:
- Add backend endpoint `POST /api/feedback`.
- Accept feedback events for candidate shown, previewed, accepted, rejected, edited, exported, and rated.
- Store events locally as JSONL for Phase 2.
- Include analysisRunId, candidateId, displayedRank, eventType, timestamp, engineVersion, and optional edited start/end.
- Do not store raw audio.
- Add frontend hooks for preview, export, and explicit good/bad rating buttons.
- Add tests for feedback validation and JSONL write behavior.
```

### Review Checklist

- Does feedback work without raw audio upload?
- Are invalid event types rejected?
- Is event schema documented?
- Does the UI remain usable if feedback logging fails?

## LLM Task Prompt: Add Research Harness

```text
Create a research harness for SplicePoint musical analysis.

Requirements:
- Add `/research` folder.
- Add Python scripts for baseline analysis using librosa where available.
- Add a JSON schema for comparing backend output against research output.
- Add documentation for installing research dependencies separately from production dependencies.
- Do not make Python dependencies required for running the Java backend.
- Add a small synthetic-audio generator for test fixtures.
```

### Review Checklist

- Research dependencies are isolated.
- Production backend still builds without Python.
- Outputs are comparable through stable JSON.
- Synthetic fixtures can test BPM/onset behavior.

## LLM Task Prompt: Add First Ranking Model Skeleton

```text
Add a first-pass learning ranker skeleton for SplicePoint.

Requirements:
- Do not replace heuristic scoring.
- Create a training-data schema from feedback events and candidate features.
- Add script to build a tabular dataset from JSONL logs.
- Add placeholder training script for logistic regression or small MLP.
- Export model metadata, feature list, and version.
- Add docs explaining that model ranking is optional and experimental.
```

### Review Checklist

- Does heuristic fallback remain active?
- Is model version tracked?
- Are feature names stable?
- Is training reproducible?
- Are raw audio files excluded by default?

---

# Implementation Priority Stack

## Immediate Next Steps

1. Fix any frontend/backend compatibility issues from the current Phase 1 merge.
2. Add stable candidate IDs and analysis run IDs.
3. Refactor analysis code into clear service modules.
4. Add score components to current candidates.
5. Add synthetic WAV tests for known transient and BPM cases.
6. Add feedback event schema documentation.

## Short-Term Build Order

```text
1. Stabilize current backend/frontend contract
2. Add score component schema
3. Add onset envelope and transient scoring
4. Add autocorrelation BPM estimate
5. Add beat-grid candidate generation
6. Add feedback endpoint and JSONL logging
7. Add Python research harness
8. Add first supervised ranker offline
9. Add optional ONNX runtime path
```

## Avoid For Now

Do not prioritize these until the DSP and feedback foundation is stable:

- transformer beat tracking
- end-to-end audio neural loop finder
- heavy browser-side ffmpeg.wasm by default
- user accounts
- cloud audio storage
- large-scale model personalization
- DAW-style editing features

These are tempting shiny machines, but the current system needs a clean musical engine before it needs chrome exhaust pipes.

---

# Recommended File Structure

## Backend

```text
backend/src/main/java/com/example/backend/
  AudioController.java
  analysis/
    AudioAnalysisService.java
    AudioDecoder.java
    AudioFeatures.java
    OnsetDetector.java
    TempoEstimator.java
    BeatGridEstimator.java
    LoopCandidateGenerator.java
    LoopScorer.java
    ScoreWeights.java
  export/
    AudioExportService.java
    WavEncoder.java
  feedback/
    FeedbackController.java
    FeedbackEvent.java
    FeedbackLogService.java
```

## Frontend

```text
frontend/
  index.html
  app.js
  styles.css
  audio/
    apiClient.js
    candidateRenderer.js
    feedbackClient.js
```

## Docs

```text
docs/
  ROADMAP.md
  MUSICAL_ANALYSIS_ROADMAP.md
  API.md
  FEEDBACK_SCHEMA.md
  ANALYSIS_ENGINE.md
  MODEL_RANKING.md
```

## Research

```text
research/
  README.md
  requirements.txt
  scripts/
    generate_synthetic_audio.py
    librosa_baseline.py
    build_training_dataset.py
    train_loop_ranker.py
  schemas/
    analysis_output.schema.json
    training_example.schema.json
```

---

# Success Metrics

## Musical Accuracy Metrics

- BPM error on synthetic click tracks
- beat-grid alignment error
- onset detection timing tolerance
- candidate seam risk
- loop candidate acceptance rate
- median manual edit distance

## Product Metrics

- time from upload to first candidate
- time to first accepted/exported loop
- export success rate
- candidate preview rate
- explicit good/bad feedback rate
- fallback/error rate by file type

## Model Metrics

- acceptance prediction ROC-AUC or PR-AUC
- ranking NDCG@3
- acceptance@1 lift over heuristic-only
- calibration error
- reduction in manual trim edits

---

# Final Direction

The next version of SplicePoint should become a hybrid MIR system:

```text
DSP generates candidates.
Heuristics explain candidates.
Users correct candidates.
Feedback trains the ranker.
The ranker improves future ordering.
The backend helps only when it earns its keep.
```

That gives SplicePoint a practical path from prototype to intelligent loop assistant without pretending one giant model can hear taste before the product has collected any taste data.
