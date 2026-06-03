# Changelog

All meaningful project changes should be recorded here in human-readable form.

This project is currently in prototype development, so dates mark documentation and architecture milestones rather than formal releases.

## Unreleased

### Added

- Documentation structure with root README landing page and deeper `docs/` index.
- Mission, architecture, setup, usage, configuration, security, testing, and troubleshooting docs.
- Contribution guidelines.
- Architecture decision records folder.

### Planned

- Clean package naming for the backend.
- API schema documentation.
- Feedback schema documentation.
- CI workflow for backend tests.
- Real BPM and beat-grid analysis.

## 2026-06-03

### Added

- Merged Phase 1 backend into `main`.
- Added Spring Boot backend under `backend/`.
- Added `/api/health`, `/api/extract`, and `/api/export` endpoints.
- Added Phase 1 heuristic loop analysis.
- Added selected-region WAV export.
- Added draggable/resizable frontend loop regions.
- Added backend tests using synthetic WAV fixtures.
- Added `docs/ROADMAP.md`.
- Added `docs/MUSICAL_ANALYSIS_ROADMAP.md`.
- Added `docs/SLM_FEEDBACK_ARCHITECTURE.md`.

### Known Limitations

- BPM and beat-grid detection are not complete.
- Candidate ranking is still heuristic.
- Java Sound format support is limited compared with FFmpeg-style decoding.
- CORS is permissive for local prototype work and must be restricted before hosted deployment.

## 2025-08-22

### Added

- Initial SplicePoint repository.
- Static WaveSurfer.js frontend prototype.
- Drag-and-drop upload area.
- Basic waveform rendering and loop-region visualization.
