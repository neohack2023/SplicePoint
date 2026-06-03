# Mission

## Project Purpose

SplicePoint exists to help creators find useful musical loop points faster.

The project combines waveform interaction, backend audio analysis, and future feedback-driven ranking so a creator can move from raw audio to a usable loop with less guessing and fewer manual trim/export cycles.

## Target Users

SplicePoint is being built for:

- music producers chopping samples
- DJs preparing loops or stems
- remixers looking for clean splice regions
- sample-pack creators building loop libraries
- audio tinkerers experimenting with musical structure
- developers researching music information retrieval workflows

## Goals

SplicePoint should:

- make loop discovery visual and fast
- suggest candidate splice regions from actual audio features
- allow users to audition, adjust, and export loops
- keep loop suggestions explainable
- keep manual override available at every stage
- support future learning from user feedback
- stay focused on loop extraction rather than becoming a full DAW

## Non-Goals

SplicePoint is not currently trying to be:

- a full audio editor
- a DAW replacement
- a cloud sample marketplace
- an automatic copyright detector
- a mastering suite
- a generative music model
- a guaranteed perfect BPM/beat-grid detector

Those features may touch nearby territory, but they should not distract from the core loop workflow.

## Current Philosophy

The project should evolve in layers:

```text
working loop workflow
→ explainable DSP heuristics
→ musical structure analysis
→ feedback logging
→ learned ranking
→ optional SLM-assisted prediction
```

Do not add heavy machinery before the basic workflow is stable.

## Guiding Principles

- Every loop suggestion must be explainable, editable, and safely exportable.
- Musical usefulness matters more than raw waveform neatness.
- User correction is signal, not failure.
- Backend complexity must earn its keep.
- The interface may be creative, but failure modes should be boring and recoverable.
