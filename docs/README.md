# Documentation Index

This folder contains the deeper project documentation for SplicePoint.

The root [`README.md`](../README.md) is the landing page. These docs are the working manual for development, usage, planning, and future contributors.

## Start Here

| Document | Purpose |
|---|---|
| [Mission](MISSION.md) | Project purpose, target users, goals, non-goals, and guiding principles |
| [Setup](SETUP.md) | Requirements, installation, local development, and startup steps |
| [Usage](USAGE.md) | How to use SplicePoint after setup |
| [Troubleshooting](TROUBLESHOOTING.md) | Common problems, likely causes, and fixes |

## Technical Docs

| Document | Purpose |
|---|---|
| [Architecture](ARCHITECTURE.md) | System design, data flow, runtime behavior, boundaries, and extension points |
| [API Reference](API.md) | Backend endpoint contract and response shapes |
| [Configuration](CONFIGURATION.md) | Runtime settings, defaults, and safe values |
| [Testing](TESTING.md) | Automated tests, manual QA, and regression expectations |
| [Security](SECURITY.md) | Trust boundaries, unsafe inputs, file handling, and safety rules |

## Planning

| Document | Purpose |
|---|---|
| [Roadmap](ROADMAP.md) | Active, planned, parked, and rejected work |
| [Musical Analysis Roadmap](MUSICAL_ANALYSIS_ROADMAP.md) | DSP, MIR, loop scoring, feedback learning, and ranking plan |
| [SLM Feedback Architecture](SLM_FEEDBACK_ARCHITECTURE.md) | Statistical and Small Language Model direction for adaptive loop prediction |
| [Decision Records](DECISIONS/) | Architecture decision records and major project tradeoffs |
| [Changelog](../CHANGELOG.md) | Human-readable project history |
| [Contributing](../CONTRIBUTING.md) | Contribution rules and expectations |

## Documentation Rule

Keep the root README short. Put implementation detail here.

When adding a feature, update the smallest relevant doc:

- setup change → `SETUP.md`
- API or component change → `API.md` and/or `ARCHITECTURE.md`
- runtime option → `CONFIGURATION.md`
- user-facing behavior → `USAGE.md`
- risk or boundary → `SECURITY.md`
- known problem → `TROUBLESHOOTING.md`
- future work → `ROADMAP.md`
- major design choice → `DECISIONS/`
