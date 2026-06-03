# 0001: Project Foundation

## Date

2026-06-03

## Context

SplicePoint is an experimental audio loop extraction tool. The project needs a clear foundation before adding deeper musical analysis, feedback learning, or SLM-assisted ranking.

The initial prototype includes a browser frontend and a Spring Boot backend. The frontend handles upload, waveform display, candidate region interaction, and export triggering. The backend handles Phase 1 audio analysis and WAV export.

## Decision

SplicePoint will focus first on a stable upload → analyze → select → export workflow.

The root README will remain a concise project landing page. Deeper technical, usage, security, testing, and roadmap details will live in `docs/`.

The project will keep `main` as the primary working branch unless a risky change requires temporary isolation.

## Alternatives Considered

### Full DAW-style editor

Rejected for now. This would add too much scope before the core loop workflow is stable.

### Browser-only architecture

Parked. Browser-side processing is useful, but the current project already has a backend and benefits from a clear API boundary.

### Backend-only processing with minimal frontend intelligence

Rejected. The frontend needs to remain interactive and editable, not just a file uploader.

## Consequences

- Documentation is organized around actual project needs, not generic repo decoration.
- Future features should preserve the core loop workflow.
- Audio analysis can grow behind the existing API contract.
- More advanced SLM/ranking work must wait until candidate IDs, score components, and feedback logs exist.
