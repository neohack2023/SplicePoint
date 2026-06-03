# SplicePoint SLM Feedback Architecture

## Purpose

This document turns the SLM research notes into an implementation direction for SplicePoint.

The important clarification is that **SLM means two useful things** in this project:

1. **Statistical Language Model**: symbolic and probabilistic sequence modeling, such as n-grams, Markov chains, Prediction by Partial Matching, and Multiple Viewpoint Systems.
2. **Small Language Model**: compact neural models that can run locally or near-locally for music understanding, captioning, ranking, and constrained symbolic reasoning.

SplicePoint should not treat an SLM as a magic loop oracle. The right design is a layered system:

```text
DSP / MIR features
→ structure and repetition features
→ statistical expectation model
→ user feedback ranker
→ optional small neural re-ranker
```

The core job is still practical: suggest better loop regions, learn from user corrections, and improve ranking over time.

---

# Core Insight

A good loop is not only an audio slice. It is a repeated or repeatable musical unit.

That means SplicePoint needs to understand:

- local acoustic events
- beat and bar timing
- repeated motifs
- structural boundaries
- seam quality
- user preference
- correction behavior

The SLM layer should sit above the DSP layer and answer questions like:

- Does this candidate behave like a complete musical phrase?
- Does this start/end pair match the repeated structure of the track?
- Did the user consistently move starts slightly before transients?
- Does this user prefer tight drum-loop boundaries or wider phrase loops?
- Which candidates should be ranked higher next time?

---

# Structural Representation Layer

## Self-Similarity Matrix

Self-Similarity Matrices are useful for detecting repeated musical structure within a track.

Given a feature sequence:

```text
X = x1, x2, ..., xN
```

SplicePoint can compare each feature vector against every other feature vector to produce a similarity matrix.

Useful patterns:

| Pattern | Meaning | SplicePoint Use |
|---|---|---|
| Diagonal blocks | homogeneous sections | stable loop zones |
| Parallel diagonal paths | repeated motifs or phrases | strong loop candidates |
| isolated blocks | contrasting sections | section boundaries |
| novelty peaks | structural change | possible splice starts |

## Practical Use

Use SSMs for medium or deep analysis mode, not necessarily for every tiny sample.

Suggested feature inputs:

- chroma vectors
- MFCCs
- spectral centroid / bandwidth
- onset envelope
- beat-synchronous features
- energy envelope

Suggested outputs:

```json
{
  "structure": {
    "repetitionScore": 0.74,
    "sectionBoundaryScore": 0.61,
    "homogeneityScore": 0.80,
    "nearestRepeatedSegmentStart": 24.0,
    "nearestRepeatedSegmentEnd": 32.0
  }
}
```

## Similarity Matrix Profile

Full SSMs have quadratic space complexity, so they can become expensive on long tracks.

For longer files, use a matrix-profile-style approach:

```text
for each subsequence:
  find nearest non-overlapping repeated subsequence
  store distance and index
```

This gives SplicePoint a lighter way to detect motifs, repeated loops, and unusual sections without storing a full matrix.

Recommended roadmap use:

- Phase 2: simple SSM for short files or beat-synchronous windows
- Phase 3: matrix profile for longer files
- Phase 4: learned structural embeddings

---

# Statistical Language Model Layer

## Why Statistical SLMs Matter

A statistical SLM models musical expectation.

For symbolic events:

```text
P(next_event | previous_events)
```

For SplicePoint, the event does not need to be a note. It can be a symbolic abstraction of audio analysis:

```text
event = {
  beatPosition,
  onsetClass,
  energyClass,
  chordClass,
  sectionClass,
  rhythmicPatternClass
}
```

This lets SplicePoint model the probability that a loop boundary is musically expected.

## Event Tokenization for Audio Loops

Convert audio features into compact symbolic tokens:

```text
beat:0 onset:strong energy:high chroma:C section:A
beat:1 onset:weak energy:mid chroma:C section:A
beat:2 onset:kick energy:high chroma:G section:A
beat:3 onset:snare energy:high chroma:G section:A
```

Then train lightweight sequence models over these tokens.

## Multiple Viewpoint System

Use separate streams of musical information instead of forcing everything into one token.

| Viewpoint | Example | Use |
|---|---|---|
| basic | beat position, onset strength, RMS bucket | direct event identity |
| derived | interval between onsets, energy delta | motion/change |
| linked | beat position + onset class | groove identity |
| threaded | downbeat-to-downbeat changes | phrase behavior |

This maps well to SplicePoint because loops are multi-dimensional: rhythm, energy, harmony, and structure all matter at once.

## Long-Term and Short-Term Models

Use three expectation models:

| Model | Trained On | Purpose |
|---|---|---|
| LTM | general loop/track corpus | broad musical expectations |
| STM | current uploaded track | piece-specific repetition |
| LTM+ | general model updated during analysis | bridge between global and local style |

For early SplicePoint, implement only the STM first:

```text
current track → token sequence → local repetition / expectation scores
```

Later, add an LTM trained from accepted loops and public-safe datasets.

## Entropy-Weighted Ensemble

If multiple models make predictions, prefer the model that is more confident in the current context.

Simple version:

```text
weight(model) = 1 / entropy(model_prediction)
```

Then:

```text
combinedScore = weighted average of model predictions
```

SplicePoint use:

- if STM strongly detects a repeated riff, trust STM more
- if current track is sparse or ambiguous, trust LTM/heuristics more
- if user behavior is consistent, trust user-specific ranker more

## PPM and PPM-Decay

Prediction by Partial Matching is a variable-order Markov model.

Useful for SplicePoint because it can adapt to repeated local patterns without needing a large neural model.

PPM-Decay adds memory decay, which is useful because recent musical events often matter more than distant ones.

SplicePoint use:

```text
recent repeated pattern = high local expectation
old unrelated section = lower expectation
```

This can help detect loop candidates that feel natural inside the current track rather than merely looking clean on the waveform.

---

# Small Language Model Layer

## What a Small Neural SLM Should Do

The small neural SLM should not replace DSP.

It should provide one or more of these services:

- candidate re-ranking
- loop-quality explanation
- style classification
- structure labeling
- symbolic constraint checking
- user preference adaptation
- lightweight captioning of why a loop works

## Candidate Re-Ranker

The first useful SLM-like model can be tiny and tabular.

Input:

```json
{
  "loopBeats": 8,
  "onsetScore": 0.91,
  "beatAlignmentScore": 0.88,
  "repetitionScore": 0.74,
  "seamRisk": 0.12,
  "userEditDistanceAverage": 0.04,
  "previousUserAcceptedSimilar": true
}
```

Output:

```json
{
  "acceptProbability": 0.83,
  "rankingDelta": 0.12,
  "explanation": "strong repeated beat-aligned phrase with low seam risk"
}
```

Recommended first models:

- logistic regression
- gradient boosted trees
- tiny MLP
- ONNX exported ranker

Do not start with a 200M+ parameter audio-language model unless the project has a real reason.

## Symbolic Constraint Checker

A constrained symbolic model can verify whether loop candidates obey basic musical rules:

- starts on downbeat
- ends on phrase boundary
- preserves meter length
- does not cut inside a strong pickup unless intended
- maintains harmonic continuity

This can be implemented before any neural model using finite-state rules.

Example:

```text
valid loop = starts on beat 1 AND duration in allowed beat lengths AND seamRisk below threshold
```

Later, a symbolic SLM can learn softer versions of these rules.

## TinyMU / Audio-Language Model Direction

Compact audio-language models are promising, but they should be treated as a later research lane.

Possible future use:

- classify uploaded audio as drum loop, melodic phrase, vocal phrase, full song, ambience, one-shot
- caption loop candidates
- detect instrumentation or mood
- provide natural-language explanations
- assist search/retrieval across a user library

Do not block Phase 2 on this.

## SymPAC / Constrained Generation Direction

Symbolic constrained generation is more relevant if SplicePoint later creates loop variations.

Possible future use:

- generate MIDI companion loops
- suggest variation loops that preserve meter/key
- enforce beat/bar constraints through finite-state machines
- prevent generated continuation drift

This is not part of the first loop extraction milestone.

---

# Feedback and Reinforcement Layer

## Feedback Types

SplicePoint should collect both explicit and implicit feedback.

| Feedback | Signal Type | Meaning |
|---|---|---|
| good/bad button | explicit | direct preference |
| export | strong implicit positive | user found it useful |
| repeated preview | implicit positive | candidate was interesting |
| instant skip | implicit negative | candidate likely bad |
| manual edit | correction signal | candidate was close but off |
| deleted region | negative | unwanted candidate |
| favorited | strong positive | high-quality loop |

## Preference Pair Generation

When a user chooses one candidate over another:

```text
winner = exported candidate
loser = shown but skipped candidate
```

This can create pairwise training examples for ranking:

```json
{
  "context": "analysisRunId",
  "winnerCandidateId": "loop_003",
  "loserCandidateId": "loop_001",
  "reason": "exported_vs_skipped"
}
```

## RLHF-Inspired Ranking

Full RLHF is too heavy for SplicePoint Phase 2.

Use the idea, not the full machinery:

```text
collect preferences
→ train reward/ranking model
→ re-rank future candidates
→ keep heuristic fallback
```

Avoid PPO-style policy optimization for now.

## DPO-Inspired Direction

Direct Preference Optimization is useful as a concept for later pairwise model tuning.

For SplicePoint, the practical equivalent is:

```text
increase score of accepted/exported loops
reduce score of rejected/skipped loops
optimize directly on preference pairs
```

This can be done with a pairwise ranking loss before any generative model exists.

## Human-In-The-Loop RL

For interactive adaptation, keep it simple:

```text
if user accepts similar boundaries repeatedly:
  increase ranking weight for those features

if user repeatedly edits starts earlier:
  learn user-specific start-offset preference

if user rejects short loops:
  lower short-loop ranking for that user/session
```

This gives SplicePoint a practical feedback loop without turning it into a research beast.

## Online Learning to Rank

Use online learning cautiously.

Start with batch updates:

```text
feedback logs → offline training → versioned ranker → deploy
```

Later, support session-local adaptation:

```text
current session edits → temporary user preference weights → reset or persist with consent
```

---

# Proposed SplicePoint Learning Pipeline

```text
1. Generate DSP candidates
2. Extract score components
3. Detect structure/repetition features
4. Assign stable candidate IDs
5. Show candidates to user
6. Log shown/previewed/edited/exported/rejected events
7. Build preference pairs
8. Train first ranking model offline
9. Export small ONNX ranker
10. Blend model score with heuristic score
```

## Blended Score

```text
finalScore =
  heuristicScore * 0.60
+ structureScore * 0.20
+ learnedPreferenceScore * 0.20
```

Early versions should keep heuristic weight dominant.

Later:

```text
finalScore =
  heuristicScore * 0.40
+ structureScore * 0.25
+ learnedPreferenceScore * 0.35
```

Only increase learned weight after validation.

---

# Candidate Feature Schema

Each loop candidate should eventually include:

```json
{
  "candidateId": "loop_001",
  "start": 8.0,
  "end": 16.0,
  "duration": 8.0,
  "loopBeats": 16,
  "heuristicScore": 0.78,
  "structureScore": 0.72,
  "learnedPreferenceScore": null,
  "confidence": 0.76,
  "scoreComponents": {
    "onsetScore": 0.91,
    "beatAlignmentScore": 0.87,
    "tempoStabilityScore": 0.80,
    "boundaryCleanlinessScore": 0.74,
    "seamRiskScore": 0.13,
    "repetitionScore": 0.68,
    "homogeneityScore": 0.73,
    "noveltyBoundaryScore": 0.59,
    "statisticalExpectationScore": 0.66,
    "userPreferenceScore": null
  },
  "reasons": [
    "beat-aligned phrase",
    "low seam risk",
    "repeated motif detected"
  ]
}
```

---

# Feedback Event Schema

```json
{
  "eventId": "evt_001",
  "analysisRunId": "run_001",
  "candidateId": "loop_001",
  "eventType": "shown | previewed | accepted | rejected | edited | exported | rated_good | rated_bad",
  "timestamp": "ISO-8601",
  "displayedRank": 1,
  "previewDurationMs": 8400,
  "editedStart": 8.031,
  "editedEnd": 15.984,
  "explicitRating": "good",
  "engineVersion": "phase-2-dsp-v1",
  "rankerVersion": "none"
}
```

---

# Human Developer Roadmap Add-On

## Phase SLM-1: Structural Features

- Add beat-synchronous feature vectors.
- Add repetition score.
- Add homogeneity score.
- Add novelty boundary score.
- Add lightweight SSM for short files.
- Add matrix-profile research note for long files.

## Phase SLM-2: Feedback Schema

- Add stable `analysisRunId`.
- Add stable `candidateId`.
- Add `POST /api/feedback` or local feedback logger.
- Log explicit and implicit events.
- Store JSONL locally for Phase 2.

## Phase SLM-3: Statistical Expectation Model

- Tokenize beat-level events.
- Build current-track STM model.
- Score loop candidates by expectation/surprisal.
- Add `statisticalExpectationScore` to candidates.

## Phase SLM-4: First Learned Ranker

- Build dataset from feedback logs.
- Train logistic/MLP/GBM ranker.
- Export ONNX model.
- Blend learned score with heuristic score.

## Phase SLM-5: Optional Small Neural SLM

- Evaluate compact audio-language model only after candidate and feedback data exist.
- Use it for classification, explanation, or search.
- Do not make it required for core loop export.

---

# LLM Developer Instructions

When an LLM agent works on the SLM layer, use these rules:

1. Do not replace DSP with an SLM.
2. Add stable IDs before adding learning.
3. Add feedback logging before adding model training.
4. Keep raw audio out of logs by default.
5. Store feature vectors and outcomes.
6. Keep score components explainable.
7. Use small ranking models before large audio-language models.
8. Version every scoring formula and model.
9. Always preserve heuristic fallback.
10. Do not add reinforcement learning machinery unless the feedback schema already exists.

## LLM Task Prompt: Add Stable IDs

```text
Add stable analysisRunId and candidateId fields to SplicePoint.

Requirements:
- Each /api/extract response must include analysisRunId.
- Each loop candidate must include candidateId.
- IDs should be deterministic within one analysis run.
- Do not change /api/export behavior.
- Update frontend to preserve selected candidateId when exporting or rating.
- Add tests for candidate ID presence.
```

## LLM Task Prompt: Add Feedback Endpoint

```text
Add a Phase 2 feedback logging endpoint.

Requirements:
- Add POST /api/feedback.
- Accept JSON feedback events for shown, previewed, edited, accepted, rejected, exported, rated_good, and rated_bad.
- Store events as JSONL for now.
- Validate required fields: analysisRunId, candidateId, eventType, timestamp, engineVersion.
- Do not store raw audio.
- Frontend should log preview, export, and explicit good/bad ratings.
- Feedback failure must not break export or playback.
```

## LLM Task Prompt: Add Structural Scoring

```text
Add structural scoring to loop candidates.

Requirements:
- Compute beat-synchronous feature vectors.
- Add repetitionScore, homogeneityScore, and noveltyBoundaryScore.
- For short files, use a simple self-similarity matrix.
- For longer files, skip SSM and return a warning until matrix-profile support exists.
- Add the scores under scoreComponents.
- Use the scores in final confidence with documented weights.
```

## LLM Task Prompt: Add Statistical SLM Prototype

```text
Add a simple statistical sequence model for loop expectation scoring.

Requirements:
- Convert beat-level features into symbolic tokens.
- Build a current-track short-term model from those tokens.
- Score candidate loop starts and ends by local expectation / surprisal.
- Add statisticalExpectationScore to scoreComponents.
- Keep this model dependency-free.
- Add tests with synthetic repeated patterns.
```

---

# Implementation Priority

Do this order:

```text
1. Stable analysisRunId and candidateId
2. Score component object
3. Feedback event schema and logging
4. Structural repetition scoring
5. Statistical SLM short-term model
6. Offline ranking dataset builder
7. First small ranker
8. ONNX export and browser/server inference
9. Optional compact audio-language model experiments
```

---

# Final Direction

The SLM strategy should make SplicePoint more adaptive, not more bloated.

The correct mental model:

```text
DSP hears the signal.
Structure finds repetition.
Statistical SLM predicts expectation.
User feedback teaches taste.
Small neural models improve ranking only after enough evidence exists.
```

That gives SplicePoint a path toward intelligent loop prediction without sacrificing the fast, practical upload-preview-export workflow.
