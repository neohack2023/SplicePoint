# SplicePoint

SplicePoint is an experimental browser-based loop extraction tool that helps creators find, preview, adjust, and export useful musical splice points from audio files.

## Mission

SplicePoint exists to make sample chopping faster, more visual, and more musically aware without turning the workflow into a full DAW.

The project goal is simple: upload audio, see likely loop candidates, audition them, adjust the region if needed, and export a clean WAV loop.

## What It Does

SplicePoint currently provides:

- a static WaveSurfer.js frontend for waveform viewing and region editing
- drag-and-drop audio upload
- backend-assisted audio analysis through Spring Boot
- loop candidate suggestions with confidence-style metadata
- selected-region auditioning and WAV export
- early backend regression tests for analysis and export behavior

## Why It Was Made

Manual loop hunting can be slow: creators scrub waveforms, guess boundaries, trim by ear, export, and repeat. SplicePoint is being built to reduce that friction by combining waveform interaction, musical-analysis heuristics, and future feedback-driven ranking.

It is designed for producers, DJs, remixers, sample pack builders, and audio tinkerers who want a focused loop utility instead of another heavyweight editing environment.

## Current Status

**Prototype / active development.**

The Phase 1 backend and frontend workflow are in place, but the analysis engine is still early. Current candidate detection uses heuristic audio features. Full BPM detection, beat-grid alignment, self-similarity analysis, feedback learning, and SLM-assisted ranking are planned but not complete.

## Quick Start

### Requirements

- Java 21
- A browser with modern JavaScript support
- Network access on first backend build so Maven can download dependencies

### Run the backend

From the repository root:

```bash
./backend/mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\backend\mvnw.cmd spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

### Open the frontend

Open this file in a browser:

```text
frontend/index.html
```

When opened from disk, the frontend sends API calls to `http://localhost:8080`.

### Run tests

```bash
./backend/mvnw test
```

## Project Structure

```text
SplicePoint/
├── README.md
├── CHANGELOG.md
├── CONTRIBUTING.md
├── backend/                 # Spring Boot backend API and audio services
├── frontend/                # Static WaveSurfer.js browser interface
└── docs/                    # Project documentation and planning notes
```

Important files:

| Path | Purpose |
|---|---|
| `frontend/index.html` | Browser UI shell |
| `frontend/app.js` | Upload, analysis, region selection, and export workflow |
| `frontend/styles.css` | Frontend layout and styling |
| `backend/pom.xml` | Spring Boot/Maven project definition |
| `backend/src/main/java/com/example/backend/AudioController.java` | REST API endpoints |
| `backend/src/main/java/com/example/backend/AudioServices.java` | Phase 1 audio analysis/export services |
| `backend/src/test/java/com/example/backend/BackendApplicationTests.java` | Backend regression tests |

## Documentation

| Document | Purpose |
|---|---|
| [Documentation Index](docs/README.md) | Main map of project docs |
| [Mission](docs/MISSION.md) | Project purpose, target users, goals, and non-goals |
| [Architecture](docs/ARCHITECTURE.md) | System design, data flow, boundaries, and extension points |
| [Setup](docs/SETUP.md) | Installation and local development instructions |
| [Usage](docs/USAGE.md) | How to use SplicePoint after setup |
| [Configuration](docs/CONFIGURATION.md) | Runtime options, defaults, and safe values |
| [Security](docs/SECURITY.md) | Input risks, trust boundaries, and safety rules |
| [Testing](docs/TESTING.md) | Test commands, manual QA, and regression expectations |
| [Roadmap](docs/ROADMAP.md) | Active, planned, parked, and rejected work |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | Common symptoms, causes, and fixes |
| [Musical Analysis Roadmap](docs/MUSICAL_ANALYSIS_ROADMAP.md) | DSP, MIR, ranking, and feedback-learning plan |
| [SLM Feedback Architecture](docs/SLM_FEEDBACK_ARCHITECTURE.md) | Statistical/Small Language Model direction for loop prediction |
| [Decision Records](docs/DECISIONS/) | Major design choices and tradeoffs |
| [Contributing](CONTRIBUTING.md) | Contribution rules and workflow expectations |
| [Changelog](CHANGELOG.md) | Human-readable project history |

## Guiding Principle

Every loop suggestion must be explainable, editable, and safely exportable.

## License

No license has been selected yet. Until a license is added, assume all rights are reserved by the repository owner.
