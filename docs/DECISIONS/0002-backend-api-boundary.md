# 0002: Backend API Boundary for Phase 1

## Date

2026-06-03

## Context

SplicePoint can theoretically process audio in several places:

- directly in the browser
- in a Java backend
- in a Python research backend
- through a future native or FFmpeg-based service
- through a future model-ranking layer

The project currently has a static frontend and Spring Boot backend. The backend exposes `/api/extract` and `/api/export`, while the frontend stays responsible for waveform interaction and selected-region control.

## Decision

Use a backend API boundary for Phase 1 while keeping the analysis engine modular enough to evolve.

The Phase 1 API contract is:

```text
POST /api/extract
file=<audio file>
→ returns file metadata, warnings, and loop candidates

POST /api/export
file=<audio file>
start=<seconds>
end=<seconds>
fadeMs=<optional milliseconds>
→ returns audio/wav
```

The frontend should not need to know whether future analysis is powered by Java heuristics, browser DSP, Python research code, or a learned ranker.

## Alternatives Considered

### Browser-only analysis and export

Useful for privacy and fewer server calls, but it does not solve all codec and future research needs. Parked as a possible future engine path.

### Backend-only UI with server-rendered pages

Rejected. SplicePoint needs direct waveform interaction in the browser.

### Heavy model-first architecture

Rejected. The project needs stable feature extraction, candidate scoring, and feedback logs before model ranking becomes useful.

## Consequences

- The current backend can evolve without rewriting the frontend workflow.
- The frontend remains interactive and editable.
- Future browser-first or model-assisted engines should plug into the same candidate schema.
- Backend upload and decode risks must be documented and handled carefully.
- CORS and upload limits must be revisited before deployment.
