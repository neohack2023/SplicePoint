# API Reference

SplicePoint currently exposes a small local-development backend API.

Base URL during local development:

```text
http://localhost:8080
```

## `GET /api/health`

Checks whether the backend is running.

### Example Response

```json
{
  "status": "ok",
  "service": "splicepoint-backend",
  "version": "0.1.0",
  "engine": "java-sound"
}
```

## `POST /api/extract`

Analyzes an uploaded audio file and returns loop candidates.

### Request

Content type:

```text
multipart/form-data
```

Fields:

| Field | Required | Description |
|---|---|---|
| `file` | yes | Audio file to analyze |

### Example Response Shape

```json
{
  "fileName": "example.wav",
  "contentType": "audio/wav",
  "byteSize": 123456,
  "durationSeconds": 8.0,
  "sampleRate": 44100,
  "channels": 2,
  "bpmEstimate": null,
  "bpmConfidence": 0.0,
  "engineStatus": "phase-1-heuristic",
  "loops": [
    {
      "start": 0.0,
      "end": 2.0,
      "duration": 2.0,
      "confidence": 0.72,
      "label": "transient candidate",
      "reasons": [
        "strong transient near start",
        "clean boundary estimate"
      ]
    }
  ],
  "warnings": [
    "BPM and beat-grid detection are not fully implemented yet; candidates use transient, energy, and boundary heuristics."
  ]
}
```

### Notes

- Current BPM fields are placeholders.
- Loop candidates are heuristic, not final musical-analysis predictions.
- WAV and AIFF are the safest Phase 1 formats.

## `POST /api/export`

Exports the selected loop region as a WAV file.

### Request

Content type:

```text
multipart/form-data
```

Fields:

| Field | Required | Description |
|---|---|---|
| `file` | yes | Original audio file |
| `start` | yes | Start time in seconds |
| `end` | yes | End time in seconds |
| `fadeMs` | no | Edge fade in milliseconds. Defaults to `5` |

### Response

Content type:

```text
audio/wav
```

The response is a downloadable WAV file.

## Error Response Shape

Failed analysis or export requests return JSON similar to:

```json
{
  "code": "export_failed",
  "message": "End time must be greater than start time."
}
```

Known error codes:

| Code | Meaning |
|---|---|
| `extract_failed` | Backend could not analyze the uploaded file |
| `export_failed` | Backend could not export the selected region |

## Future API TODOs

- Add stable `analysisRunId`.
- Add stable `candidateId` for loop candidates.
- Add `scoreComponents` object to candidates.
- Add `POST /api/feedback`.
- Add explicit API schemas once the Phase 2 candidate contract stabilizes.
